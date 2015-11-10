package alien4cloud.tosca.parser.impl.advanced;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.ImplementationArtifact;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;
import alien4cloud.utils.MapUtil;

import com.google.common.io.Files;

@Component
public class ImplementationArtifactParser extends DefaultDeferredParser<ImplementationArtifact> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;

    @Override
    public ImplementationArtifact parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            String artifactReference = ((ScalarNode) node).getValue();

            Path artifactPath = Paths.get(artifactReference);
            String extension = Files.getFileExtension(artifactPath.getFileName().toString());

            String type = null;
            if (extension != null) {
                ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
                IndexedArtifactType indexedType = getFromArchiveRoot(archiveRoot, extension);

                if (indexedType == null) {
                    GetMultipleDataResult<IndexedArtifactType> artifactType = alienDao.find(IndexedArtifactType.class,
                            MapUtil.newHashMap(new String[] { "fileExt" }, new String[][] { new String[] { extension } }), 1);
                    if (artifactType != null && artifactType.getData() != null && artifactType.getData().length > 0) {
                        Set<CSARDependency> archiveDependencies = archiveRoot.getArchive().getDependencies();
                        for (IndexedArtifactType foundType : artifactType.getData()) {
                            if (archiveDependencies.contains(new CSARDependency(foundType.getArchiveName(), foundType.getArchiveVersion()))) {
                                type = foundType.getElementId();
                                break;
                            }
                        }
                    }
                    if (type == null) {
                        context.getParsingErrors().add(new ParsingError(ErrorCode.UNKNOWN_IMPLEMENTATION_ARTIFACT, "Implementation artifact",
                                node.getStartMark(), "No artifact type in the repository references the artifact's extension", node.getEndMark(), extension));
                        type = "unknown";
                    }
                } else {
                    type = indexedType.getElementId();
                }
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
