package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
public class BoundParser extends ScalarParser {
    private static final String UNBOUNDED = "unbounded";

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        String value = super.parse(node, context);
        return UNBOUNDED.equals(value) ? String.valueOf(Integer.MAX_VALUE) : value;
    }
}
