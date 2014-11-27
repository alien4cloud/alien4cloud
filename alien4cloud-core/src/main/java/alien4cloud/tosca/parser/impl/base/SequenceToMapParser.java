package alien4cloud.tosca.parser.impl.base;

import java.util.Map;

import lombok.AllArgsConstructor;

import org.elasticsearch.common.collect.Maps;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Parse a yaml sequence into a {@link Map}
 *
 * @param <T> The type of the values of the map.
 */
@AllArgsConstructor
public class SequenceToMapParser<T> implements INodeParser<Map<String, T>> {
    private TypeNodeParser<T> valueParser;
    /** The tosca type of the map. */
    private String toscaType;

    @Override
    public Map<String, T> parse(Node node, ParsingContextExecution context) {
        if (node instanceof SequenceNode) {
            SequenceNode sequenceNode = (SequenceNode) node;
            Map<String, T> sequenceMap = Maps.newHashMap();
            for (Node elementNode : sequenceNode.getValue()) {
                if (elementNode instanceof MappingNode) {
                    MappingNode mappingNode = (MappingNode) elementNode;
                    String key = ((ScalarNode) mappingNode.getValue().get(0).getKeyNode()).getValue();
                    T value = valueParser.parse(mappingNode, context);
                    sequenceMap.put(key, value);
                } else {
                    ParserUtils.addTypeError(node, context.getParsingErrors(), toscaType);
                }
            }

            return sequenceMap;
        }

        ParserUtils.addTypeError(node, context.getParsingErrors(), toscaType);
        return null;
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return valueParser.isDeferred(context);
    }
}