package alien4cloud.tosca.parser.impl.v12.advanced;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.io.Files;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.ImplementationArtifact;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;

@Component("implementationArtifactParser-v12")
public class ImplementationArtifactParser extends DefaultDeferredParser<ImplementationArtifact> {
    @Resource
    private ICSARRepositorySearchService repositorySearchService;

    @Override
    public ImplementationArtifact parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            String artifactReference = ((ScalarNode) node).getValue();

            Path artifactPath = Paths.get(artifactReference);
            String extension = Files.getFileExtension(artifactPath.getFileName().toString());

            String type = null;
            ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
            IndexedArtifactType indexedType = getFromArchiveRoot(archiveRoot, extension);
            if (indexedType == null) {
                IndexedArtifactType artifactType = repositorySearchService.getElementInDependencies(IndexedArtifactType.class,
                        QueryBuilders.termQuery("fileExt", extension), archiveRoot.getArchive().getDependencies());
                if (artifactType != null) {
                    type = artifactType.getElementId();
                }
                if (type == null) {
                    context.getParsingErrors().add(new ParsingError(ErrorCode.UNKNOWN_ARTIFACT, "Implementation artifact", node.getStartMark(),
                            "No artifact type in the repository references the artifact's extension", node.getEndMark(), extension));
                    type = "unknown";
                }
            } else {
                type = indexedType.getElementId();
            }
            ImplementationArtifact artifact = new ImplementationArtifact();
            artifact.setArtifactRef(artifactReference);
            artifact.setArtifactType(type);
            return artifact;
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Artifact definition");
        }
        return null;
    }

    private IndexedArtifactType getFromArchiveRoot(ArchiveRoot archiveRoot, String extension) {
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
