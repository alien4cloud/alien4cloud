package alien4cloud.tosca.parser.impl.base;

import alien4cloud.tosca.parser.*;
import com.google.common.collect.Maps;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Map;

/**
 * Switch parser allows to define multiple sub-parsers based on the type of the yaml node.
 */
public class SwitchParser<T> implements INodeParser<T> {
    private String toscaType;
    private Map<Class<?>, INodeParser<T>> parsers;

    public SwitchParser(Map<Class<?>, INodeParser<T>> parsers) {
        if (parsers == null) {
            this.parsers = Maps.newHashMap();
        } else {
            this.parsers = parsers;
        }
    }

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        INodeParser<T> parser = parsers.get(node.getClass());
        if (parser == null) {
            ParserUtils.addTypeError(node, context.getParsingErrors(), toscaType);
            return null;
        }
        return parser.parse(node, context);
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return false;
    }
}