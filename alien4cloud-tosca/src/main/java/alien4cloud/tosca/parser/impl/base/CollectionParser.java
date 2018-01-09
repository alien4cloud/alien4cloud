package alien4cloud.tosca.parser.impl.base;

import java.util.Collection;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import lombok.AllArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Setter
public abstract class CollectionParser<T> implements INodeParser<Collection<T>> {
    private INodeParser<T> valueParser;
    /** The tosca type of the list. */
    private String toscaType;
    /** In case the list is created from a map, optional value to inject the key into the value object. */
    private String keyPath;

    @Override
    public Collection<T> parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            return doParseFromMap((MappingNode) node, context);
        } else if (node instanceof SequenceNode) {
            return doParse((SequenceNode) node, context);
        } else if (node instanceof ScalarNode) {
            // single value in the list
            return doParse((ScalarNode) node, context);
        }
        ParserUtils.addTypeError(node, context.getParsingErrors(), toscaType);
        return null;
    }

    private Collection<T> doParse(ScalarNode node, ParsingContextExecution context) {
        Collection<T> collection = getCollectionInstance();
        T value = valueParser.parse(node, context);
        if (value != null) {
            collection.add(value);
        }
        return collection;
    }

    private Collection<T> doParse(SequenceNode node, ParsingContextExecution context) {
        Collection<T> collection = getCollectionInstance();
        Object parent = context.getParent();
        for (Node valueNode : node.getValue()) {
            T value;
            if (keyPath != null && valueNode instanceof MappingNode && ((MappingNode) valueNode).getValue().size() == 1) {
                NodeTuple tuple = ((MappingNode) valueNode).getValue().get(0);
                value = objectFromTuple(tuple, context);
            } else {
                value = valueParser.parse(valueNode, context);
            }
            if (value != null) {
                context.setParent(parent, value);
                collection.add(value);
            }
            context.setParent(parent);
        }
        return collection;
    }

    private Collection<T> doParseFromMap(MappingNode node, ParsingContextExecution context) {
        Collection<T> collection = getCollectionInstance();
        if (keyPath != null) { // we parse a map into a list and must
            for (NodeTuple entry : node.getValue()) {
                T value = objectFromTuple(entry, context);
                if (value != null) {
                    collection.add(value);
                }
            }
        } else { // we parse a list with a single value
            T value = valueParser.parse(node, context);
            collection.add(value);
        }
        return collection;
    }

    private T objectFromTuple(NodeTuple tuple, ParsingContextExecution context) {
        String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
        T value;
        value = valueParser.parse(tuple.getValueNode(), context);
        if (value != null) {
            BeanWrapper valueWrapper = new BeanWrapperImpl(value);
            valueWrapper.setPropertyValue(keyPath, key);
        }
        return value;
    }

    protected abstract Collection<T> getCollectionInstance();

}
