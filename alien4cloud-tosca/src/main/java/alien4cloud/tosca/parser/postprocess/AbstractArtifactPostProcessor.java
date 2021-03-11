package alien4cloud.tosca.parser.postprocess;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.AbstractArtifact;
import org.alien4cloud.tosca.model.definitions.RepositoryDefinition;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;

import java.util.Map;

/**
 * Perform post processing and validation of an artifact.
 *
 * Check type, try to infer it from file extension if not explicitly specified.
 * Manage repository.
 */
public abstract class AbstractArtifactPostProcessor implements IPostProcessor<AbstractArtifact> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private ICSARRepositorySearchService repositorySearchService;

    @Override
    public void process(AbstractArtifact instance) {
        Node node = ParsingContextExecution.getObjectToNodeMap().get(instance);

        Map<Object,Node> map = ParsingContextExecution.getObjectToNodeMap();
        postProcessArtifactRef(node, instance.getArtifactRef());

        ArchiveRoot archiveRoot = ParsingContextExecution.getRootObj();
        // If archive name is already defined (by the type for example then don't override it)
        if (StringUtils.isBlank(instance.getArchiveName())) {
            instance.setArchiveName(archiveRoot.getArchive().getName());
            instance.setArchiveVersion(archiveRoot.getArchive().getVersion());
        }

        if (instance.getArtifactType() == null) {
            // try to get type from extension
            instance.setArtifactType(getArtifactTypeByExtension(instance.getArtifactRef(), node, archiveRoot));
        } else {
            // check the type reference
            referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getArtifactType(), ArtifactType.class));
        }
        if (instance.getRepositoryName() != null) {
            RepositoryDefinition repositoryDefinition = archiveRoot.getRepositories() != null ? archiveRoot.getRepositories().get(instance.getRepositoryName())
                    : null;
            if (repositoryDefinition == null) {
                // Sometimes the information about repository has already been filled in the parent type
                if (instance.getRepositoryURL() == null) {
                    ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.UNKNOWN_REPOSITORY, "Implementation artifact",
                            node.getStartMark(), "Repository definition not found", node.getEndMark(), instance.getArtifactRepository()));
                }
            } else {
                instance.setRepositoryURL(repositoryDefinition.getUrl());
                instance.setRepositoryCredential(repositoryDefinition.getCredential() != null ? repositoryDefinition.getCredential().getValue() : null);
                instance.setRepositoryName(repositoryDefinition.getId());
                instance.setArtifactRepository(repositoryDefinition.getType());
            }
        }
    }

    protected abstract void postProcessArtifactRef(Node node, String artifactReference);

    private String getArtifactTypeByExtension(String artifactReference, Node node, ArchiveRoot archiveRoot) {
        int dotIndex = artifactReference.lastIndexOf('.');
        String extension = (dotIndex == -1) ? "" : artifactReference.substring(dotIndex + 1);
        String type = null;
        ArtifactType indexedType = getFromArchiveRootWithExtension(archiveRoot, extension);
        if (indexedType == null) {
            ArtifactType artifactType = repositorySearchService.getElementInDependencies(ArtifactType.class, archiveRoot.getArchive().getDependencies(),
                    "fileExt", extension);
            if (artifactType != null) {
                type = artifactType.getElementId();
            }
            if (type == null) {
                if (node != null) {
                    // Node will be null if coming from a cloned artifact (node merge)
                    // In this case , no error reporting because it has been done
                    // on the super node.
                    ParsingContextExecution.getParsingErrors().add(new ParsingError(
                            ErrorCode.TYPE_NOT_FOUND,
                            "Implementation artifact",
                            node.getStartMark(),
                            "No artifact type in the repository references the artifact's extension",
                            node.getEndMark(),
                            extension
                    ));
                }
                type = "unknown";
            }
        } else {
            type = indexedType.getElementId();
        }
        return type;
    }

    private ArtifactType getFromArchiveRootWithExtension(ArchiveRoot archiveRoot, String extension) {
        if (archiveRoot == null || archiveRoot.getArtifactTypes() == null || extension == null) {
            return null;
        }
        for (ArtifactType artifactType : archiveRoot.getArtifactTypes().values()) {
            if (artifactType.getFileExt() != null && artifactType.getFileExt().contains(extension)) {
                return artifactType;
            }
        }
        return null;
    }
}