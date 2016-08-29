package alien4cloud.tosca.parser.postprocess;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.AbstractArtifact;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.RepositoryDefinition;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Perform post processing and validation of an artifact.
 *
 * Check type, try to infer it from file extension if not explicitly specified.
 * Manage repository.
 */
@Component
public class ArtifactPostProcessor implements IPostProcessor<AbstractArtifact> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private ICSARRepositorySearchService repositorySearchService;

    @Override
    public void process(AbstractArtifact instance) {
        ArchiveRoot archiveRoot = ParsingContextExecution.getRootObj();
        instance.setArchiveName(archiveRoot.getArchive().getName());
        instance.setArchiveVersion(archiveRoot.getArchive().getVersion());
        Node node = ParsingContextExecution.getObjectToNodeMap().get(instance);
        if (instance.getArtifactType() == null) {
            // try to get type from extension
            instance.setArtifactType(getArtifactTypeByExtension(instance.getArtifactRef(), node, archiveRoot));
        } else {
            // check the reference
            referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getArtifactType(), IndexedArtifactType.class));
        }
        if (instance.getArtifactRepository() != null) {
            RepositoryDefinition repositoryDefinition = archiveRoot.getRepositories().get(instance.getArtifactRepository());
            if (repositoryDefinition == null) {
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.UNKNOWN_REPOSITORY, "Implementation artifact", node.getStartMark(),
                        "Repository definition not found", node.getEndMark(), instance.getArtifactRepository()));
            } else {
                instance.setRepositoryURL(repositoryDefinition.getUrl());
                instance.setRepositoryCredentials(repositoryDefinition.getCredentials());
                instance.setRepositoryName(repositoryDefinition.getId());
                instance.setArtifactRepository(repositoryDefinition.getType());
            }
        }
    }

    private String getArtifactTypeByExtension(String artifactReference, Node node, ArchiveRoot archiveRoot) {
        int dotIndex = artifactReference.lastIndexOf('.');
        String extension = (dotIndex == -1) ? "" : artifactReference.substring(dotIndex + 1);
        String type = null;
        IndexedArtifactType indexedType = getFromArchiveRootWithExtension(archiveRoot, extension);
        if (indexedType == null) {
            IndexedArtifactType artifactType = repositorySearchService.getElementInDependencies(IndexedArtifactType.class,
                    QueryBuilders.termQuery("fileExt", extension), archiveRoot.getArchive().getDependencies());
            if (artifactType != null) {
                type = artifactType.getElementId();
            }
            if (type == null) {
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Implementation artifact", node.getStartMark(),
                        "No artifact type in the repository references the artifact's extension", node.getEndMark(), extension));
                type = "unknown";
            }
        } else {
            type = indexedType.getElementId();
        }
        return type;
    }

    private IndexedArtifactType getFromArchiveRootWithExtension(ArchiveRoot archiveRoot, String extension) {
        if (archiveRoot == null || archiveRoot.getArtifactTypes() == null || extension == null) {
            return null;
        }
        for (IndexedArtifactType artifactType : archiveRoot.getArtifactTypes().values()) {
            if (artifactType.getFileExt() != null && artifactType.getFileExt().contains(extension)) {
                return artifactType;
            }
        }
        return null;
    }
}