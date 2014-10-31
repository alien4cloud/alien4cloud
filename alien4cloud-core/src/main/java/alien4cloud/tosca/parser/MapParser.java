package alien4cloud.tosca.parser;

import java.util.Map;

import lombok.AllArgsConstructor;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import com.google.common.collect.Maps;

@AllArgsConstructor
public class MapParser<T> implements INodeParser<Map<String, T>> {
    private INodeParser<T> valueParser;
    /** The tosca type of the map. */
    private String toscaType;
    /** Optional value to inject the key into the value object. */
    private String keyPath;

    public MapParser(INodeParser<T> valueParser, String toscaType) {
        this(valueParser, toscaType, null);
    }

    @Override
    public Map<String, T> parse(Node node, ParsingContext context) {
        if (node instanceof MappingNode) {
            return doParse((MappingNode) node, context);
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), toscaType);
        }
        return null;
    }

    private Map<String, T> doParse(MappingNode node, ParsingContext context) {
        Map<String, T> map = Maps.newHashMap();
        for (NodeTuple entry : node.getValue()) {
            String key = ParserUtils.getScalar(entry.getKeyNode(), context.getParsingErrors());
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

    @Override
    public boolean isDeffered() {
        return false;
    }
}