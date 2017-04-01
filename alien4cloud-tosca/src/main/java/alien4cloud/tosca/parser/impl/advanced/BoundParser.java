package alien4cloud.tosca.parser.impl.advanced;

import org.alien4cloud.tosca.normative.constants.RangeConstants;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

@Component
public class BoundParser extends ScalarParser {

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        String value = super.parse(node, context);
        if (value == null) {
            value = "";
        }
        return RangeConstants.UNBOUNDED.equalsIgnoreCase(value) ? String.valueOf(Integer.MAX_VALUE) : value;
    }
}
