package alien4cloud.tosca.parser;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.tosca.parser.impl.ErrorCode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Utility class to help with parsing.
 */
public final class ParserUtils {

    /**
     * Utility to get a scalar.
     * 
     * @param node The node from which to get a scalar value.
     * @param context The parsing execution context in which to add errors in which to add errors in case the node is not a scalar node.
     * @return The Scalar value or null if the node is not a scalar node.
     */
    public static String getScalar(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getValue().trim();
        }
        addTypeError(node, context.getParsingErrors(), "scalar");
        return null;
    }

    /**
     * Build a map while parsing a {@link MappingNode} assuming that all tuples are scalars (both key and value). Other entries are ignored but warned.
     * 
     * @param ignoredKeys ignore these keys (no warning for them !)
     */
    public static Map<String, String> parseStringMap(MappingNode mappingNode, ParsingContextExecution context, String... ignoredKeys) {
        Map<String, String> result = Maps.newHashMap();
        List<NodeTuple> mappingNodeValues = mappingNode.getValue();
        for (NodeTuple entry : mappingNodeValues) {
            if (!(entry.getKeyNode() instanceof ScalarNode)) {
                ParsingError err = new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, "Parsing a MappingNode as a Map", entry
                        .getKeyNode().getStartMark(), "The key of this tuple should be a scalar", entry.getKeyNode().getEndMark(), "");
                context.getParsingErrors().add(err);
                continue;
            }
            if (!(entry.getValueNode() instanceof ScalarNode)) {
                if (!ArrayUtils.contains(ignoredKeys, getScalar(entry.getKeyNode(), context))) {
                    ParsingError err = new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, "Parsing a MappingNode as a Map", entry
                            .getKeyNode().getStartMark(), "The value of this tuple should be a scalar", entry.getValueNode().getEndMark(),
                            ((ScalarNode) entry.getKeyNode()).getValue());
                    context.getParsingErrors().add(err);
                }
                continue;
            }
            // ok both key and value are scalar
            String k = ((ScalarNode) entry.getKeyNode()).getValue();
            String v = ((ScalarNode) entry.getValueNode()).getValue();
            result.put(k, v);
        }
        return result;
    }

    public static Object parse(Node node) {
        if (node == null) {
            return null;
        } else if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getValue();
        } else if (node instanceof SequenceNode) {
            return parseSequence((SequenceNode) node);
        } else if (node instanceof MappingNode) {
            return parseMap((MappingNode) node);
        } else {
            throw new InvalidArgumentException("Unknown type of node " + node.getClass().getName());
        }
    }

    public static List<Object> parseSequence(SequenceNode sequenceNode) {
        List<Object> result = Lists.newArrayList();
        for (Node node : sequenceNode.getValue()) {
            result.add(parse(node));
        }
        return result;
    }

    public static Map<String, Object> parseMap(MappingNode mappingNode) {
        Map<String, Object> result = Maps.newHashMap();
        for (NodeTuple entry : mappingNode.getValue()) {
            String key = ((ScalarNode) entry.getKeyNode()).getValue();
            result.put(key, parse(entry.getValueNode()));
        }
        return result;
    }

    /**
     * Add an invalid type {@link ParsingError} to the given parsing errors list.
     * 
     * @param node The node that is causing the type error.
     * @param parsingErrors The parsing errors in which to add the error.
     * @param expectedType The type that was actually expected.
     */
    public static void addTypeError(Node node, List<ParsingError> parsingErrors, String expectedType) {
        parsingErrors.add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Invalid type syntax", node.getStartMark(), "Expected the type to match tosca type", node
                .getEndMark(), expectedType));
    }
}