package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.alien4cloud.tosca.model.definitions.AbstractArtifact;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

public abstract class ArtifactParser<T extends AbstractArtifact> implements INodeParser<T> {
    @Resource
    private ScalarParser scalarParser;
    @Resource
    private ArtifactReferenceParser artifactReferenceParser;

    private ArtifactReferenceMissingMode artifactReferenceMissingMode;

    public enum ArtifactReferenceMissingMode {
        RAISE_WARNING, RAISE_ERROR, NONE
    }

    public ArtifactParser(ArtifactReferenceMissingMode artifactReferenceMissingMode) {
        this.artifactReferenceMissingMode = artifactReferenceMissingMode;
    }

    public ArtifactParser() {
        this.artifactReferenceMissingMode = ArtifactReferenceMissingMode.RAISE_ERROR;
    }

    private INodeParser<String> getValueParser(String key) {
        return "file".equals(key) ? artifactReferenceParser : scalarParser;
    }

    private void checkArtifactReference(AbstractArtifact artifact, Node node, ParsingContextExecution context) {
        if (artifact.getArtifactRef() == null) {
            switch (artifactReferenceMissingMode) {
            case RAISE_ERROR:
                context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Implementation artifact", node.getStartMark(),
                        "No artifact reference is defined, 'file' is mandatory in a long notation artifact definition", node.getEndMark(), null));
                break;
            case RAISE_WARNING:
                context.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRESOLVED_ARTIFACT, "Deployment artifact", node.getStartMark(),
                                "No artifact reference is defined, user will have to define / override in order to make ", node.getEndMark(), null));
                break;
            default:
                break;
            }
        }
    }

    /**
     * Method to be called by implementations providing an instance of the artifact to parse.
     * 
     * @param artifact The artifact instance (can be a DeploymentArtifact or an ImplementationArtifact).
     * @param node The yaml node.
     * @param context The context.
     * @return The parsed artifact.
     */
    protected T doParse(T artifact, Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            String artifactReference = ((ScalarNode) node).getValue();
            artifact.setArtifactRef(artifactReference);
            checkArtifactReference(artifact, node, context);
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
                    artifact.setArtifactRepository(value);
                    break;
                case "type":
                    artifact.setArtifactType(value);
                    break;
                default:
                    context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNOWN_ARTIFACT_KEY, null, node.getStartMark(),
                            "Unrecognized key while parsing implementation artifact", node.getEndMark(), key));
                }
            }
            checkArtifactReference(artifact, node, context);
            return artifact;
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Artifact definition");
        }
        return null;
    }
}