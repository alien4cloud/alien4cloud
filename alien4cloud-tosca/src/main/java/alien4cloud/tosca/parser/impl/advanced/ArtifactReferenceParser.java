package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultParser;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import javax.annotation.Resource;

@Component
public class ArtifactReferenceParser extends DefaultParser<String> {
    @Resource
    private ScalarParser scalarParser;

    @Override
    public String parse(Node node, ParsingContextExecution context) {
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