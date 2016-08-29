package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

@Component
public class ArtifactReferenceParser implements INodeParser<String> {
    @Resource
    private ScalarParser scalarParser;

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        // FIXME this is to support artifact as inputs (using get_input), maybe we should do something better here and support a PropertyValue in the artifact.
        if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getValue();
        } else if (node instanceof MappingNode) {
            MappingNode mappingNode = (MappingNode) node;
            Node function = mappingNode.getValue().get(0).getKeyNode();
            Node value = mappingNode.getValue().get(0).getValueNode();
            return "{ " + scalarParser.parse(function, context) + ": " + scalarParser.parse(value, context) + " }";
        }
        return null;
    }
}