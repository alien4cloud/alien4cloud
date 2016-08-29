package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Parser that state that get_artifact is not supported by alien and will be ignored.
 */
@Component
public class FailGetArtifactParser implements INodeParser<Object> {
    @Override
    public Object parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            NodeTuple nodeTuple = ((MappingNode) node).getValue().get(0);
            if (nodeTuple.getKeyNode() instanceof ScalarNode) {
                String key = ((ScalarNode) nodeTuple.getKeyNode()).getValue();
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, "Ignored field during import",
                        nodeTuple.getKeyNode().getStartMark(), "tosca key is not recognized", nodeTuple.getValueNode().getEndMark(), key));
            }
        }
        return null;
    }
}