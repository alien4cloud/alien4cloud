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

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.exceptions.UnresolvableArtifactException;
import alien4cloud.model.components.AbstractArtifact;
import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.Interface;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.repository.services.RepositoryService;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.InputArtifactUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ArchivePostProcessor {

    @Resource
    private RepositoryService repositoryService;

    private interface ArchivePathResolver extends AutoCloseable {

        Path resolve(String artifactReference);

        void close();
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

        @Override
        public void close() {
            try {
                fileSystem.close();
            } catch (Exception e) {
                // Ignored
            }
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

        @Override
        public void close() {
            // Do nothing
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

    private boolean hasInputArtifacts(ParsingResult<ArchiveRoot> parsedArchive) {
        return parsedArchive.getResult().getTopology() != null && parsedArchive.getResult().getTopology().getInputArtifacts() != null;
    }

    private void processArtifact(ArchivePathResolver archivePathResolver, AbstractArtifact artifact, ParsingResult<ArchiveRoot> parsedArchive) {
        if (StringUtils.isBlank(artifact.getArtifactRef())) {
            parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.INVALID_ARTIFACT_REFERENCE, "Empty artifact reference", null,
                    "Artifact's reference " + artifact.getArtifactRef() + " is empty", null, artifact.getArtifactRef()));
            return;
        }
        if (!(parsedArchive.getResult().getArchive().getName().equals(artifact.getArchiveName())
                && parsedArchive.getResult().getArchive().getVersion().equals(artifact.getArchiveVersion()))) {
            // if the artifact is not defined in the current archive then we don't have to perform validation.
            return;
        }
        String inputArtifactId = InputArtifactUtil.getInputArtifactId(artifact);
        if (StringUtils.isNotBlank(inputArtifactId) && hasInputArtifacts(parsedArchive)
                && !parsedArchive.getResult().getTopology().getInputArtifacts().containsKey(inputArtifactId)) {
            // The input artifact id does not exist in the topology's definition
            parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.INVALID_ARTIFACT_REFERENCE, "Invalid artifact reference", null,
                    "Artifact's reference " + artifact.getArtifactRef() + " is not valid", null, artifact.getArtifactRef()));
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
            interfaceMap.values().stream().filter(interfazz -> interfazz.getOperations() != null)
                    .forEach(interfazz -> interfazz.getOperations().values().stream().filter(operation -> operation.getImplementationArtifact() != null)
                            .forEach(operation -> processArtifact(archivePathResolver, operation.getImplementationArtifact(), parsedArchive)));
        }
    }

    private <T extends IndexedArtifactToscaElement> void processTypes(ArchivePathResolver archivePathResolver, Map<String, T> types,
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

    private <T extends AbstractTemplate> void processTemplate(ArchivePathResolver archivePathResolver, T template, ParsingResult<ArchiveRoot> parsedArchive) {
        if (template.getArtifacts() != null) {
            template.getArtifacts().values().stream().filter(artifact -> artifact != null)
                    .forEach(artifact -> processArtifact(archivePathResolver, artifact, parsedArchive));
        }
        processInterfaces(archivePathResolver, template.getInterfaces(), parsedArchive);
    }

    private ArchivePathResolver createPathResolver(Path archive) {
        if (Files.isRegularFile(archive)) {
            try {
                return new ZipArchivePathResolver(archive);
            } catch (Exception e) {
                throw new UnresolvableArtifactException("Csar's temporary file is not accessible as a Zip", e);
            }
        } else {
            return new DirArchivePathResolver(archive);
        }
    }

    private void processInputArtifact(ArchivePathResolver archivePathResolver, ParsingResult<ArchiveRoot> parsedArchive) {
        if (hasInputArtifacts(parsedArchive)) {
            parsedArchive.getResult().getTopology().getInputArtifacts().values().stream()
                    // If artifact reference is null it means it's to be uploaded with the GUI
                    .filter(inputArtifact -> StringUtils.isNotBlank(inputArtifact.getArtifactRef()))
                    .forEach(inputArtifact -> this.processArtifact(archivePathResolver, inputArtifact, parsedArchive));
        }
    }

    private void processArtifacts(final Path archive, ParsingResult<ArchiveRoot> parsedArchive) {
        try (ArchivePathResolver archivePathResolver = createPathResolver(archive)) {
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
}