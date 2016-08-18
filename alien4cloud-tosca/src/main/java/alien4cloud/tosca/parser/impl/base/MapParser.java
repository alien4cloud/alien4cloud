package alien4cloud.tosca.parser.impl.base;

import java.util.Map;

import lombok.AllArgsConstructor;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.mapping.DefaultParser;

import com.google.common.collect.Maps;

@AllArgsConstructor(suppressConstructorProperties = true)
public class MapParser<T> extends DefaultParser<Map<String, T>> {
    private INodeParser<T> valueParser;
    /** The tosca type of the map. */
    private String toscaType;
    /** Optional value to inject the key into the value object. */
    private String keyPath;

    public MapParser(INodeParser<T> valueParser, String toscaType) {
        this(valueParser, toscaType, null);
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return valueParser.isDeferred(context);
    }

    @Override
    public int getDeferredOrder(ParsingContextExecution context) {
        return valueParser.getDeferredOrder(context);
    }

    @Override
    public Map<String, T> parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            return doParse((MappingNode) node, context);
        } else if (node instanceof ScalarNode) {
            String scalarValue = ((ScalarNode) node).getValue();
            if (scalarValue == null || scalarValue.trim().isEmpty()) {
                // node is just not defined, return null.
                return null;
            }
        }
        ParserUtils.addTypeError(node, context.getParsingErrors(), toscaType);
        return null;
    }

    private Map<String, T> doParse(MappingNode node, ParsingContextExecution context) {
        Map<String, T> map = Maps.newLinkedHashMap();
        for (NodeTuple entry : node.getValue()) {
            String key = ParserUtils.getScalar(entry.getKeyNode(), context);
            T value = null;
            value = valueParser.parse(entry.getValueNode(), context);
            if (value != null) {
                if (keyPath != null) {
                    BeanWrapper valueWrapper = new BeanWrapperImpl(value);
                    valueWrapper.setPropertyValue(keyPath, key);
                }
                map.put(key, value);
            }
        }
        return map;
    }

}