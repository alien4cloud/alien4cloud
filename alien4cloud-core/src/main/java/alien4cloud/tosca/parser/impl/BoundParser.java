package alien4cloud.tosca.parser.impl;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.ParsingContext;

@Component
public class BoundParser extends ScalarParser {
    private static final String UNBOUNDED = "unbounded";

    @Override
    public String parse(Node node, ParsingContext context) {
        String value = super.parse(node, context);
        return UNBOUNDED.equals(value) ? String.valueOf(Integer.MAX_VALUE) : value;
    }
}
