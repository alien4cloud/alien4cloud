package alien4cloud.tosca.parser.impl.base;

import lombok.AllArgsConstructor;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Parser implementation that delegates parsing to a parser referenced in the parser registry based on the type key.
 */
@AllArgsConstructor
public class ReferencedParser<T> implements INodeParser<T> {
    private String typeName;

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        INodeParser delegate = context.getRegistry().get(typeName);
        return (T) delegate.parse(node, context);
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        INodeParser delegate = context.getRegistry().get(typeName);
        return delegate.isDeferred(context);
    }
}
