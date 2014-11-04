package alien4cloud.tosca.parser;

import java.util.Collection;

import lombok.AllArgsConstructor;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

@AllArgsConstructor
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
        for (Node valueNode : node.getValue()) {
            T value = valueParser.parse(valueNode, context);
            if (value != null) {
                collection.add(value);
            }
        }
        return collection;
    }

    private Collection<T> doParseFromMap(MappingNode node, ParsingContextExecution context) {
        Collection<T> collection = getCollectionInstance();
        for (NodeTuple entry : node.getValue()) {
            String key = ParserUtils.getScalar(entry.getKeyNode(), context.getParsingErrors());
            T value = null;
            value = valueParser.parse(entry.getValueNode(), context);
            if (value != null) {
                if (keyPath != null) {
                    BeanWrapper valueWrapper = new BeanWrapperImpl(value);
                    valueWrapper.setPropertyValue(keyPath, key);
                }
                collection.add(value);
            }
        }
        return collection;
    }

    protected abstract Collection<T> getCollectionInstance();

    @Override
    public boolean isDeffered() {
        return false;
    }
}
