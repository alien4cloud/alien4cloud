package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

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
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.NormativeCredentialConstant;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.PropertyUtil;

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

        // check dependencies / imports
        processImports(archiveRoot);

        // Post process all types from the archive and update their list of parent as well as merge them with their parent types.
        processTypes(archiveRoot);

        // Then process the topology
        topologyPostProcessor.process(archiveRoot.getTopology());
        processRepositoriesDefinitions(archiveRoot.getRepositories());
    }

    private void processImports(ArchiveRoot archiveRoot) {
        if (archiveRoot.getArchive().getDependencies() == null) {
            return;
        }
        Set<CSARDependency> dependencies = archiveRoot.getArchive().getDependencies();

        // add a parsing error if there is a version conflict between a transitive and a direct dependency
        for (CSARDependency dependency : dependencies) {
            Node node = ParsingContextExecution.getObjectToNodeMap().get(dependency);
            Csar csar = ToscaContext.get().getArchive(dependency.getName(), dependency.getVersion());
            if (csar != null && csar.getDependencies() != null) {
                csar.getDependencies().forEach(transitiveDependency -> {
                    dependencies.stream()
                            .filter(directDependency -> Objects.equals(directDependency.getName(), transitiveDependency.getName())
                                    && !Objects.equals(directDependency.getVersion(), transitiveDependency.getVersion()))
                            .findFirst().ifPresent(currentDependency -> {
                                ParsingContextExecution.getParsingErrors()
                                        .add(new ParsingError(ErrorCode.DEPENDENCY_VERSION_CONFLICT, csar.getId(), node.getStartMark(),
                                                AlienUtils.prefixWith(":", transitiveDependency.getVersion(), transitiveDependency.getName()),
                                                node.getEndMark(), AlienUtils.prefixWith(":", currentDependency.getVersion(), currentDependency.getName())));
                            });
                });
            }
        }
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

        safe(archiveRoot.getDataTypes()).values().stream().forEach(toscaTypePostProcessor);
        safe(archiveRoot.getArtifactTypes()).values().stream().forEach(toscaTypePostProcessor);
        safe(archiveRoot.getCapabilityTypes()).values().stream().forEach(toscaTypePostProcessor);
        safe(archiveRoot.getRelationshipTypes()).values().stream().peek(toscaTypePostProcessor).forEach(toscaArtifactTypePostProcessor);
        safe(archiveRoot.getNodeTypes()).values().stream().peek(toscaTypePostProcessor).peek(nodeTypePostProcessor).forEach(toscaArtifactTypePostProcessor);

    }
}