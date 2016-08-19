package alien4cloud.tosca;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.deployment.exceptions.UnresolvableArtifactException;
import alien4cloud.model.components.AbstractArtifact;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.repository.services.RepositoryService;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ArchivePostProcessor {

    @Resource
    private RepositoryService repositoryService;

    private interface ArchivePathResolver {
        Path resolve(String artifactReference);
    }

    private class ZipArchivePathResolver implements ArchivePathResolver {

        private FileSystem fileSystem;

        private ZipArchivePathResolver(Path archive) throws IOException {
            fileSystem = FileSystems.newFileSystem(archive, null);
        }

        @Override
        public Path resolve(String artifactReference) {
            return fileSystem.getPath(artifactReference);
        }
    }

    private class DirArchivePathResolver implements ArchivePathResolver {

        private Path archive;

        private DirArchivePathResolver(Path archive) {
            this.archive = archive;
        }

        @Override
        public Path resolve(String artifactReference) {
            return archive.resolve(artifactReference);
        }
    }

    /**
     * Post process the archive: For every definition of the model it fills the id fields in the TOSCA elements from the key of the elements map.
     * 
     * @param parsedArchive The archive to post process
     */
    public ParsingResult<ArchiveRoot> process(Path archive, ParsingResult<ArchiveRoot> parsedArchive) {
        processTopology(parsedArchive);
        processArtifacts(archive, parsedArchive);
        return parsedArchive;
    }

    private void processTopology(ParsingResult<ArchiveRoot> parsedArchive) {
        Topology topology = parsedArchive.getResult().getTopology();
        if (topology != null) {
            TopologyUtils.normalizeAllNodeTemplateName(topology, parsedArchive);
        }
    }

    private void processLocalArtifact(ArchivePathResolver archivePathResolver, AbstractArtifact artifact, ParsingResult<ArchiveRoot> parsedArchive) {
        if (artifact.getArtifactRef() == null || artifact.getArtifactRef().isEmpty()) {
            return; // Artifact may not be specified and may be set in alien4cloud.
        }
        if (!Files.exists(archivePathResolver.resolve(artifact.getArtifactRef()))) {
            parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.INVALID_ARTIFACT_REFERENCE, "Invalid artifact reference", null,
                    "CSAR's artifact does not exist", null, artifact.getArtifactRef()));
        }
    }

    private void processArtifact(ArchivePathResolver archivePathResolver, AbstractArtifact artifact, ParsingResult<ArchiveRoot> parsedArchive) {
        if (!(parsedArchive.getResult().getArchive().getName().equals(artifact.getArchiveName())
                && parsedArchive.getResult().getArchive().getVersion().equals(artifact.getArchiveVersion()))) {
            // if the artifact is not defined in the current archive then we don't have to perform validation.
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
                artifact.getRepositoryCredentials())) {
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

    private void processInterfaces(ArchivePathResolver archivePathResolver, Map<String, Interface> interfaceMap, ParsingResult<ArchiveRoot> parsedArchive) {
        if (interfaceMap != null) {
            for (Interface interfazz : interfaceMap.values()) {
                if (interfazz.getOperations() != null) {
                    for (Operation operation : interfazz.getOperations().values()) {
                        if (operation.getImplementationArtifact() != null) {
                            processArtifact(archivePathResolver, operation.getImplementationArtifact(), parsedArchive);
                        }
                    }
                }
            }
        }
    }

    private <T extends IndexedArtifactToscaElement> void processTypes(ArchivePathResolver archivePathResolver, Map<String, T> types,
            ParsingResult<ArchiveRoot> parsedArchive) {
        if (types != null) {
            for (T type : types.values()) {
                if (type.getArtifacts() != null) {
                    for (DeploymentArtifact artifact : type.getArtifacts().values()) {
                        if (artifact != null) {
                            processArtifact(archivePathResolver, artifact, parsedArchive);
                        }
                    }
                }
                processInterfaces(archivePathResolver, type.getInterfaces(), parsedArchive);
            }
        }
    }

    private <T extends AbstractTemplate> void processTemplate(ArchivePathResolver archivePathResolver, T template, ParsingResult<ArchiveRoot> parsedArchive) {
        if (template.getArtifacts() != null) {
            for (DeploymentArtifact artifact : template.getArtifacts().values()) {
                if (artifact != null) {
                    processArtifact(archivePathResolver, artifact, parsedArchive);
                }
            }
        }
        processInterfaces(archivePathResolver, template.getInterfaces(), parsedArchive);
    }

    private void processArtifacts(final Path archive, ParsingResult<ArchiveRoot> parsedArchive) {
        ArchivePathResolver archivePathResolver;
        if (Files.isRegularFile(archive)) {
            try {
                archivePathResolver = new ZipArchivePathResolver(archive);
            } catch (Exception e) {
                throw new UnresolvableArtifactException("Csar's temporary file is not accessible as a Zip", e);
            }
        } else {
            archivePathResolver = new DirArchivePathResolver(archive);
        }
        try {
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
        } finally {
            if (archivePathResolver instanceof ZipArchivePathResolver) {
                try {
                    ((ZipArchivePathResolver) archivePathResolver).fileSystem.close();
                } catch (IOException ignored) {
                }
            }
        }

    }
}