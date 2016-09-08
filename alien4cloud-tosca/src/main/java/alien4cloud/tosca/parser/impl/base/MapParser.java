package alien4cloud.tosca.parser.impl.base;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.collect.Maps;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapParser<T> implements INodeParser<Map<String, T>> {
    @Resource
    private ScalarParser scalarParser;
    private INodeParser<T> valueParser;
    /** The tosca type of the map. */
    private String toscaType;
    /** Optional value to inject the key into the value object. */
    private String keyPath;

    public MapParser(INodeParser<T> valueParser, String toscaType) {
        this.valueParser = valueParser;
        this.toscaType = toscaType;
        this.keyPath = null;
    }

    public MapParser(INodeParser<T> valueParser, String toscaType, String keyPath) {
        this.valueParser = valueParser;
        this.toscaType = toscaType;
        this.keyPath = keyPath;
    }

    protected void setValueParser(INodeParser<T> valueParser) {
        this.valueParser = valueParser;
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
            String key = scalarParser.parse(entry.getKeyNode(), context);
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