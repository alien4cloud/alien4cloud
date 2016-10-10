package org.alien4cloud.tosca.catalog;

import static alien4cloud.utils.AlienUtils.safe;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.AbstractArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.apache.commons.lang.StringUtils;

import alien4cloud.repository.services.RepositoryService;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.InputArtifactUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Post process an archive after parsing to inject workspace and check if artifacts exists.
 */
@Slf4j
public abstract class AbstractArchivePostProcessor {
    @Resource
    private RepositoryService repositoryService;

    public interface ArchivePathChecker extends AutoCloseable {
        boolean exists(String artifactReference);

        void close();
    }

    /**
     * Post process the archive: For every definition of the model it fills the id fields in the TOSCA elements from the key of the elements map.
     *
     * @param parsedArchive The archive to post process
     */
    public ParsingResult<ArchiveRoot> process(Path archive, ParsingResult<ArchiveRoot> parsedArchive, String workspace) {
        String hash = FileUtil.deepSHA1(archive);
        parsedArchive.getResult().getArchive().setHash(hash);
        parsedArchive.getResult().getArchive().setWorkspace(workspace);

        // FIXME how should we manage hash for the topology tempalte ?
        processTopology(parsedArchive.getResult().getArchive().getWorkspace(), parsedArchive);
        // Injext archive hash in every indexed node type.
        processTypes(parsedArchive.getResult());
        processArtifacts(archive, parsedArchive);
        return parsedArchive;
    }

    /**
     * Inject the workspace in indexed elements.
     *
     * @param archiveRoot The archive out of parsing
     */
    private void processTypes(ArchiveRoot archiveRoot) {
        processTypes(archiveRoot.getArchive().getWorkspace(), archiveRoot.getArtifactTypes());
        processTypes(archiveRoot.getArchive().getWorkspace(), archiveRoot.getCapabilityTypes());
        processTypes(archiveRoot.getArchive().getWorkspace(), archiveRoot.getDataTypes());
        processTypes(archiveRoot.getArchive().getWorkspace(), archiveRoot.getNodeTypes());
        processTypes(archiveRoot.getArchive().getWorkspace(), archiveRoot.getRelationshipTypes());
    }

    private void processTypes(String workspace, Map<String, ? extends AbstractToscaType> elements) {
        for (AbstractToscaType element : safe(elements).values()) {
            element.setWorkspace(workspace);
        }
    }

    private void processTopology(String workspace, ParsingResult<ArchiveRoot> parsedArchive) {
        Topology topology = parsedArchive.getResult().getTopology();
        if (topology != null) {
            topology.setArchiveName(parsedArchive.getResult().getArchive().getName());
            topology.setArchiveVersion(parsedArchive.getResult().getArchive().getVersion());
            topology.setWorkspace(workspace);
            TopologyUtils.normalizeAllNodeTemplateName(topology, parsedArchive);
        }
    }

    private void processLocalArtifact(ArchivePathChecker archivePathResolver, AbstractArtifact artifact, ParsingResult<ArchiveRoot> parsedArchive) {
        if (artifact.getArtifactRef() == null || artifact.getArtifactRef().isEmpty()) {
            return; // Artifact may not be specified and may be set in alien4cloud.
        }
        if (!archivePathResolver.exists(artifact.getArtifactRef())) {
            parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.INVALID_ARTIFACT_REFERENCE, "Invalid artifact reference", null,
                    "CSAR's artifact does not exist", null, artifact.getArtifactRef()));
        }
    }

    private boolean hasInputArtifacts(ParsingResult<ArchiveRoot> parsedArchive) {
        return parsedArchive.getResult().getTopology() != null && parsedArchive.getResult().getTopology().getInputArtifacts() != null;
    }

    private void processArtifact(ArchivePathChecker archivePathResolver, AbstractArtifact artifact, ParsingResult<ArchiveRoot> parsedArchive) {
        if (!(parsedArchive.getResult().getArchive().getName().equals(artifact.getArchiveName())
                && parsedArchive.getResult().getArchive().getVersion().equals(artifact.getArchiveVersion()))) {
            // if the artifact is not defined in the current archive then we don't have to perform validation.
            return;
        }
        // Else also inject the workspace
        String inputArtifactId = InputArtifactUtil.getInputArtifactId(artifact);
        if (StringUtils.isNotBlank(inputArtifactId) && hasInputArtifacts(parsedArchive)) {
            if (!parsedArchive.getResult().getTopology().getInputArtifacts().containsKey(inputArtifactId)) {
                // The input artifact id does not exist in the topology's definition
                parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.INVALID_ARTIFACT_REFERENCE, "Invalid artifact reference", null,
                        "Artifact's reference " + artifact.getArtifactRef() + " is not valid", null, artifact.getArtifactRef()));
            }
            return;
        }
        URL artifactURL = null;
        if (artifact.getRepositoryName() == null) {
            // Short notation
            try {
                // Test if it's an URL
                artifactURL = new URL(artifact.getArtifactRef());
            } catch (MalformedURLException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Archive artifact validation - Processing local artifact {}", artifact);
                }
                // Not a URL then must be a relative path to a file inside the csar
                processLocalArtifact(archivePathResolver, artifact, parsedArchive);
                return;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Archive artifact validation - Processing remote artifact {}", artifact);
        }
        if (!repositoryService.canResolveArtifact(artifact.getArtifactRef(), artifact.getRepositoryURL(), artifact.getArtifactRepository(),
                artifact.getRepositoryCredential())) {
            if (artifactURL != null) {
                try (InputStream ignored = artifactURL.openStream()) {
                    // In a best effort try in a generic manner to obtain the artifact
                } catch (IOException e) {
                    parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.INVALID_ARTIFACT_REFERENCE, "Invalid artifact reference", null,
                            "Artifact's reference " + artifact.getArtifactRef() + " is not valid", null, artifact.getArtifactRef()));
                }
            } else {
                parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.UNRESOLVED_ARTIFACT, "Unresolved artifact", null,
                        "Artifact " + artifact.getArtifactRef() + " cannot be resolved", null, artifact.getArtifactRef()));
            }
        }
    }

    private void processInterfaces(ArchivePathChecker archivePathResolver, Map<String, Interface> interfaceMap, ParsingResult<ArchiveRoot> parsedArchive) {
        if (interfaceMap != null) {
            interfaceMap.values().stream().filter(interfazz -> interfazz.getOperations() != null)
                    .forEach(interfazz -> interfazz.getOperations().values().stream().filter(operation -> operation.getImplementationArtifact() != null)
                            .forEach(operation -> processArtifact(archivePathResolver, operation.getImplementationArtifact(), parsedArchive)));
        }
    }

    private <T extends AbstractInstantiableToscaType> void processTypes(ArchivePathChecker archivePathResolver, Map<String, T> types,
            ParsingResult<ArchiveRoot> parsedArchive) {
        if (types != null) {
            for (T type : types.values()) {
                if (type.getArtifacts() != null) {
                    type.getArtifacts().values().stream().filter(artifact -> artifact != null)
                            .forEach(artifact -> processArtifact(archivePathResolver, artifact, parsedArchive));
                }
                processInterfaces(archivePathResolver, type.getInterfaces(), parsedArchive);
            }
        }
    }

    private <T extends AbstractTemplate> void processTemplate(ArchivePathChecker archivePathResolver, T template, ParsingResult<ArchiveRoot> parsedArchive) {
        if (template.getArtifacts() != null) {
            template.getArtifacts().values().stream().filter(artifact -> artifact != null)
                    .forEach(artifact -> processArtifact(archivePathResolver, artifact, parsedArchive));
        }
        processInterfaces(archivePathResolver, template.getInterfaces(), parsedArchive);
    }

    private void processInputArtifact(ArchivePathChecker archivePathResolver, ParsingResult<ArchiveRoot> parsedArchive) {
        if (hasInputArtifacts(parsedArchive)) {
            parsedArchive.getResult().getTopology().getInputArtifacts().values().stream()
                    // If artifact reference is null it means it's to be uploaded with the GUI
                    .filter(inputArtifact -> StringUtils.isNotBlank(inputArtifact.getArtifactRef()))
                    .forEach(inputArtifact -> this.processArtifact(archivePathResolver, inputArtifact, parsedArchive));
        }
    }

    private void processArtifacts(final Path archive, ParsingResult<ArchiveRoot> parsedArchive) {
        try (ArchivePathChecker archivePathResolver = createPathChecker(archive)) {
            // Check if input artifacts are correctly set
            processInputArtifact(archivePathResolver, parsedArchive);
            // Process deployment artifact / implementation artifact for types
            processTypes(archivePathResolver, parsedArchive.getResult().getNodeTypes(), parsedArchive);
            processTypes(archivePathResolver, parsedArchive.getResult().getRelationshipTypes(), parsedArchive);
            // Process topology
            Topology topology = parsedArchive.getResult().getTopology();
            if (topology != null && topology.getNodeTemplates() != null) {
                for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                    processTemplate(archivePathResolver, nodeTemplate, parsedArchive);
                    if (nodeTemplate.getRelationships() != null) {
                        for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                            processTemplate(archivePathResolver, relationshipTemplate, parsedArchive);
                        }
                    }
                }
            }
        }
    }

    /**
     * Create an instance of ArchivePathChecker to check if local artifacts indeed exists in archive.
     *
     * @param archive The archive path.
     * @return An instance of archive path checker.
     */
    protected abstract ArchivePathChecker createPathChecker(Path archive);
}
