package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.AbstractArtifact;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.RepositoryDefinition;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;

public abstract class ArtifactParser<T extends AbstractArtifact> extends DefaultDeferredParser<T> {

    @Resource
    private ICSARRepositorySearchService repositorySearchService;

    @Resource
    private ScalarParser scalarParser;

    @Resource
    private ArtifactReferenceParser artifactReferenceParser;

    private INodeParser<String> getValueParser(String key) {
        if ("file".equals(key)) {
            return artifactReferenceParser;
        } else {
            return scalarParser;
        }
    }

    private String getArtifactTypeByExtension(Node node, String artifactReference, ArchiveRoot archiveRoot, ParsingContextExecution context) {
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
                context.getParsingErrors().add(new ParsingError(ErrorCode.UNKNOWN_ARTIFACT, "Implementation artifact", node.getStartMark(),
                        "No artifact type in the repository references the artifact's extension", node.getEndMark(), extension));
                type = "unknown";
            }
        } else {
            type = indexedType.getElementId();
        }
        return type;
    }

    protected T doParse(T artifact, Node node, ParsingContextExecution context) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        if (node instanceof ScalarNode) {
            String artifactReference = ((ScalarNode) node).getValue();
            artifact.setArtifactRef(artifactReference);
            artifact.setArtifactType(getArtifactTypeByExtension(node, artifactReference, archiveRoot, context));
            return artifact;
        } else if (node instanceof MappingNode) {
            MappingNode mappingNode = (MappingNode) node;
            for (NodeTuple nodeTuple : mappingNode.getValue()) {
                String key = scalarParser.parse(nodeTuple.getKeyNode(), context);
                String value = getValueParser(key).parse(nodeTuple.getValueNode(), context);
                switch (key) {
                case "file":
                    artifact.setArtifactRef(value);
                    break;
                case "repository":
                    RepositoryDefinition repositoryDefinition = archiveRoot.getRepositories().get(value);
                    if (repositoryDefinition == null) {
                        context.getParsingErrors().add(new ParsingError(ErrorCode.UNKNOWN_REPOSITORY, "Implementation artifact", node.getStartMark(),
                                "Repository definition not found", node.getEndMark(), value));
                    } else {
                        artifact.setRepositoryURL(repositoryDefinition.getUrl());
                        artifact.setRepositoryCredentials(repositoryDefinition.getCredentials());
                        artifact.setRepositoryName(repositoryDefinition.getId());
                        artifact.setArtifactRepository(repositoryDefinition.getType());
                    }
                    break;
                case "type":
                    IndexedArtifactType artifactType = getFromArchiveRootWithId(archiveRoot, value);
                    if (artifactType == null) {
                        artifactType = repositorySearchService.getElementInDependencies(IndexedArtifactType.class, value,
                                archiveRoot.getArchive().getDependencies());
                    }
                    if (artifactType == null) {
                        context.getParsingErrors().add(new ParsingError(ErrorCode.UNKNOWN_ARTIFACT, "Implementation artifact", node.getStartMark(),
                                "No artifact type in the repository references the artifact's type", node.getEndMark(), value));
                        artifact.setArtifactType("unknown");
                    } else {
                        artifact.setArtifactType(artifactType.getElementId());
                    }
                    break;
                default:
                    context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNOWN_ARTIFACT_KEY, null, node.getStartMark(),
                            "Unrecognized key while parsing implementation artifact", node.getEndMark(), key));
                }
            }
            if (artifact.getArtifactRef() == null) {
                context.getParsingErrors()
                        .add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Implementation artifact", node.getStartMark(),
                                "No artifact reference is defined, 'file' is mandatory in a long notation implementation artifact definition",
                                node.getEndMark(), null));
            } else if (artifact.getArtifactType() == null) {
                artifact.setArtifactType(getArtifactTypeByExtension(node, artifact.getArtifactRef(), archiveRoot, context));
            }
            return artifact;
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Artifact definition");
        }
        return null;
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

    private IndexedArtifactType getFromArchiveRootWithId(ArchiveRoot archiveRoot, String id) {
        if (archiveRoot == null || archiveRoot.getArtifactTypes() == null || id == null) {
            return null;
        }
        return archiveRoot.getArtifactTypes().get(id);
    }
}
