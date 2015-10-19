package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

@Component
public class BoundParser extends ScalarParser {
    private static final String UNBOUNDED = "unbounded";

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        String value = super.parse(node, context);
        if (value == null) {
            value = "";
        }
        return UNBOUNDED.equals(value.toLowerCase()) ? String.valueOf(Integer.MAX_VALUE) : value;
    }
}
