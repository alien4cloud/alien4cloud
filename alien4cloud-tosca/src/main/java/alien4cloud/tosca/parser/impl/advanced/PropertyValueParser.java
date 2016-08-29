package alien4cloud.tosca.parser.impl.advanced;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import com.google.common.collect.Lists;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Parse a tosca Scalar unit field.
 */
@Component
public class PropertyValueParser implements INodeParser<Object> {
    private static final Map<String, Double> factorMap = new HashMap<String, Double>(5);
    private static final Pattern scalarUnitPattern = Pattern.compile("([0-9.]+)\\s*([a-zA-Z]+)");
    static {
        factorMap.put("b", 1d);
        factorMap.put("kb", 1000d);
        factorMap.put("mb", 1000000d);
        factorMap.put("gb", 1000000000d);
        factorMap.put("tb", 1000000000000d);
    }

    private final double factor;

    public PropertyValueParser() {
        this("b");
    }

    /**
     * Create a scalar unit parser.
     * 
     * @param targetFactor The factor as expected in the object model.
     */
    public PropertyValueParser(String targetFactor) {
        factor = factorMap.get(targetFactor.toLowerCase());
    }

    @Override
    public Object parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            String scalarValue = ParserUtils.getScalar(node, context);
            // parse to get the number value and the unit value
            Matcher matcher = scalarUnitPattern.matcher(scalarValue);

            if (matcher.matches()) {
                Double value = Double.valueOf(matcher.group(1));
                String unitValue = matcher.group(2).toLowerCase();

                Double unitFactor = factorMap.get(unitValue);
                if (unitFactor == null) {
                    context.getParsingErrors().add(new ParsingError(ErrorCode.INVALID_SCALAR_UNIT, "Unable to parse Tosca scalar-unit.", node.getStartMark(),
                            "Unit is not a valid tosca unit, should be one of (B, kB, MB, GB, TB).", node.getEndMark(), scalarValue));
                    return null;
                }

                return new Double(value.doubleValue() * unitFactor.doubleValue() / factor).toString();
            }
            return scalarValue;
        } else if (node instanceof SequenceNode) {
            SequenceNode sequenceNode = (SequenceNode) node;
            List<Object> valueList = Lists.newArrayList();
            for (Node valueNode : sequenceNode.getValue()) {
                Object value = parse(valueNode, context);
                valueList.add(value);
            }
            return valueList;
        } else if (node instanceof MappingNode) {
            context.getParsingErrors().add(new ParsingError(ErrorCode.ALIEN_MAPPING_ERROR,
                    "Alien is currently not able to manage complex objects as property values.", node.getStartMark(), "", node.getEndMark(), ""));
        }
        return null;
    }

}