package alien4cloud.tosca.parser.impl.base;

import java.util.Map;

import lombok.AllArgsConstructor;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;

import com.google.common.collect.Maps;

/**
 * Parser implementation that delegates parsing to a parser referenced in the parser registry based on the type key.
 */
@AllArgsConstructor
public class ReferencedParser implements INodeParser {
    private String typeName;

    @Override
    public Object parse(Node node, ParsingContextExecution context) {
        INodeParser delegate = context.getRegistry().get(typeName);
        return delegate.parse(node, context);
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        INodeParser delegate = context.getRegistry().get(typeName);
        return delegate.isDeferred(context);
    }
}
