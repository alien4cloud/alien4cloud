package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.RepositoryDefinition;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.normative.constants.NormativeCredentialConstant;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Sets;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.PropertyUtil;
import alien4cloud.utils.VersionUtil;

/**
 * Performs validation and post processing of a TOSCA archive.
 */
@Component
public class ArchiveRootPostProcessor implements IPostProcessor<ArchiveRoot> {
    @Resource
    private DerivedFromPostProcessor derivedFromPostProcessor;
    @Resource
    private ToscaTypePostProcessor toscaTypePostProcessor;
    @Resource
    private NodeTypePostProcessor nodeTypePostProcessor;
    @Resource
    private ToscaArtifactTypePostProcessor toscaArtifactTypePostProcessor;
    @Resource
    private TopologyPostProcessor topologyPostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;
    @Resource
    private PolicyTypePostProcessor policyTypePostProcessor;
    @Value("${features.archive_indexer.accept_missing_requirement:#{false}}")
    private boolean acceptMissingRequirementDependency;

    /**
     * Perform validation of a Tosca archive.
     *
     * @param archiveRoot The archive to validate and post process.
     */
    public void process(ArchiveRoot archiveRoot) {
        // Register the archive in the context to ensure that all types are mapped to the TOSCA context.
        // In alien4cloud archives are referenced by id and version however this is not required by TOSCA, the following code set/unset temporary ids and reset
        // them to avoid registration issue.
        String archiveName = archiveRoot.getArchive().getName();
        String archiveVersion = archiveRoot.getArchive().getVersion();
        archiveRoot.getArchive().setYamlFilePath(ParsingContextExecution.getFileName());
        archiveRoot.getArchive().setToscaDefinitionsVersion(ParsingContextExecution.getDefinitionVersion());
        if (archiveName == null) {
            archiveRoot.getArchive().setName(ParsingContextExecution.getFileName());
        }
        if (archiveVersion == null) {
            archiveRoot.getArchive().setVersion("undefined");
        }
        // All type validation may require local archive types, so we need to register the current archive.
        ToscaContext.get().register(archiveRoot);

        doProcess(archiveRoot);

        // reset to TOSCA template value (in case they where changed)
        archiveRoot.getArchive().setName(archiveName);
        archiveRoot.getArchive().setVersion(archiveVersion);
    }

    private void doProcess(ArchiveRoot archiveRoot) {
        // Note: no post processing has to be done on repositories
        processImports(archiveRoot);
        // Post process all types from the archive and update their list of parent as well as merge them with their parent types.
        processTypes(archiveRoot);

        // Then process the topology
        topologyPostProcessor.process(archiveRoot.getTopology());
        processRepositoriesDefinitions(archiveRoot.getRepositories());
    }

    /**
     * Process imports within the archive and compute its complete dependency set.
     * Resolve all dependency version conflicts using the following rules:
     * <ul>
     * <li>If two direct dependencies conflict with each other, use the latest version</li>
     * <li>If a transitive dependency conflicts with a direct dependency, use the direct dependency version</li>
     * <li>If two transitive dependency conflict with each other, use the latest version.</li>
     * </ul>
     *
     * @param archiveRoot The archive to process.
     */
    private void processImports(ArchiveRoot archiveRoot) {
        if (archiveRoot.getArchive().getDependencies() == null || archiveRoot.getArchive().getDependencies().isEmpty()) {
            return;
        }
        // Dependencies defined in the import section only
        // These should override transitive deps regardless of type of conflict ?
        Set<CSARDependency> dependencies = archiveRoot.getArchive().getDependencies();

        // Ensure the archive does not import itself
        Csar archive = archiveRoot.getArchive();
        if (dependencies.contains(new CSARDependency(archive.getName(), archive.getVersion(), archive.getHash()))) {
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.CSAR_IMPORT_ITSELF,
                    AlienUtils.prefixWith(":", archive.getVersion(), archive.getName()), null, "Import itself", null, null));
        }

        /*
         * Three types of conflicts :
         * - A transitive dep has a different version than a direct dependency => Force transitive to direct version
         * - Transitive dependencies with the same name and different version are used => Use latest
         * - Direct dependencies with the same name and different version are used => Error or use latest ?
         */

        // 1. Resolve all direct dependencies using latest version
        dependencies.removeIf(dependency -> dependencyConflictsWithLatest(dependency, dependencies));

        // Compute all distinct transitives dependencies
        final Set<CSARDependency> transitiveDependencies = new HashSet<>(
                dependencies.stream().map(csarDependency -> ToscaContext.get().getArchive(csarDependency.getName(), csarDependency.getVersion(), acceptMissingRequirementDependency))
                        .filter(csar -> csar != null).map(Csar::getDependencies).filter(c -> c != null).reduce(Sets::union).orElse(Collections.emptySet()));

        // 2. Resolve all transitive vs. direct dependencies conflicts using the direct dependency's version
        transitiveDependencies.removeIf(transitiveDependency -> dependencyConflictsWithDirect(transitiveDependency, dependencies));

        // 3. Resolve all transitive dependencies conflicts using latest version
        transitiveDependencies.removeIf(transitiveDependency -> dependencyConflictsWithLatest(transitiveDependency, transitiveDependencies));

        // Merge all dependencies (direct + transitives)
        final Set<CSARDependency> mergedDependencies = new HashSet<>(Sets.union(dependencies, transitiveDependencies));
        archiveRoot.getArchive().setDependencies(mergedDependencies);

        // Update Tosca context with the complete dependency set
        ToscaContext.get().resetDependencies(mergedDependencies);
    }

    /**
     * Check for dependency conflicts between a transitive and a set of direct dependencies.
     *
     * @param transitiveDependency The dependency to check
     * @param dependencies The set of dependency to validate it against - assuming those are direct dependencies.
     * @return <code>true</code> if the given dependency is present in the Set in a different version.
     */
    private boolean dependencyConflictsWithDirect(CSARDependency transitiveDependency, Set<CSARDependency> dependencies) {
        return dependencies.stream()
                .filter(directDep -> Objects.equals(directDep.getName(), transitiveDependency.getName())
                        && !Objects.equals(directDep.getVersion(), transitiveDependency.getVersion()))
                .findFirst() // As we resolved direct dependencies conflicts earlier, there can only be one direct dependency that conflicts
                .map(conflictingDependency -> {
                    // Log the dependency conflict as a warning.
                    ParsingContextExecution.getParsingErrors()
                            .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.TRANSITIVE_DEPENDENCY_VERSION_CONFLICT,
                                    AlienUtils.prefixWith(":", conflictingDependency.getVersion(), conflictingDependency.getName()), null,
                                    AlienUtils.prefixWith(":", transitiveDependency.getVersion(), transitiveDependency.getName()), null,
                                    conflictingDependency.getVersion()));
                    // Resolve conflict by using the direct dependency version - delete the transitive dependency
                    return true;
                }).orElse(false);
    }

    /**
     * Check dependencies for version conflicts, and add a warning if one is found.
     *
     * @param dependency The dependency to verify.
     * @param dependencies The set of dependencies it belongs to.
     * @return <code>true</code> if the given dependency is present in the Set in a newer version.
     */
    private boolean dependencyConflictsWithLatest(CSARDependency dependency, Set<CSARDependency> dependencies) {
        return dependencies.stream().anyMatch(csarDependency -> {
            if (Objects.equals(dependency.getName(), csarDependency.getName())
                    && VersionUtil.compare(dependency.getVersion(), csarDependency.getVersion()) < 0) {
                ParsingContextExecution.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.DEPENDENCY_VERSION_CONFLICT,
                                AlienUtils.prefixWith(":", dependency.getVersion(), dependency.getName()), null,
                                AlienUtils.prefixWith(":", csarDependency.getVersion(), csarDependency.getName()), null, null));
                return true;
            } else {
                return false;
            }
        });
    }

    private void processRepositoriesDefinitions(Map<String, RepositoryDefinition> repositories) {
        if (MapUtils.isNotEmpty(repositories)) {
            DataType credentialType = ToscaContext.get(DataType.class, NormativeCredentialConstant.DATA_TYPE);
            repositories.values().forEach(repositoryDefinition -> {
                if (repositoryDefinition.getCredential() != null) {
                    credentialType.getProperties().forEach((propertyName, propertyDefinition) -> {
                        // Fill with default value
                        if (!repositoryDefinition.getCredential().getValue().containsKey(propertyName)) {
                            AbstractPropertyValue defaultValue = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(propertyDefinition);
                            if (defaultValue instanceof PropertyValue) {
                                repositoryDefinition.getCredential().getValue().put(propertyName, ((PropertyValue) defaultValue).getValue());
                            }
                        }
                    });
                    Node credentialNode = ParsingContextExecution.getObjectToNodeMap().get(repositoryDefinition.getCredential());
                    PropertyDefinition propertyDefinition = new PropertyDefinition();
                    propertyDefinition.setType(NormativeCredentialConstant.DATA_TYPE);
                    propertyValueChecker.checkProperty("credential", credentialNode, repositoryDefinition.getCredential(), propertyDefinition,
                            repositoryDefinition.getId());
                }
            });
        }
    }

    private void processTypes(ArchiveRoot archiveRoot) {
        // First of all we have to manage derived from post processor that will be applied to the map of resources as it manage dependency ordering.
        derivedFromPostProcessor.process(archiveRoot.getDataTypes());
        derivedFromPostProcessor.process(archiveRoot.getArtifactTypes());
        derivedFromPostProcessor.process(archiveRoot.getCapabilityTypes());
        derivedFromPostProcessor.process(archiveRoot.getRelationshipTypes());
        derivedFromPostProcessor.process(archiveRoot.getNodeTypes());
        derivedFromPostProcessor.process(archiveRoot.getPolicyTypes());

        safe(archiveRoot.getDataTypes()).values().stream().forEach(toscaTypePostProcessor);
        safe(archiveRoot.getArtifactTypes()).values().stream().forEach(toscaTypePostProcessor);
        safe(archiveRoot.getCapabilityTypes()).values().stream().forEach(toscaTypePostProcessor);
        safe(archiveRoot.getRelationshipTypes()).values().stream().peek(toscaTypePostProcessor).forEach(toscaArtifactTypePostProcessor);
        safe(archiveRoot.getNodeTypes()).values().stream().peek(toscaTypePostProcessor).peek(nodeTypePostProcessor).forEach(toscaArtifactTypePostProcessor);
        safe(archiveRoot.getPolicyTypes()).values().stream().peek(toscaTypePostProcessor).forEach(policyTypePostProcessor);
    }
}
