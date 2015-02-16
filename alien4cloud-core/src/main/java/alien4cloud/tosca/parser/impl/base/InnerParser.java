package alien4cloud.tosca.parser.impl.base;

import lombok.AllArgsConstructor;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Very simple scalar parser that just returns the value as string.
 */
@AllArgsConstructor
public class InnerParser implements INodeParser<Object> {

    private String type;

    private boolean deferred;

    private int deferredOrder;

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        if (deferred) {
            return true;
        } else {
            INodeParser<?> innerNodeParser = getParser(context);
            return innerNodeParser.isDeferred(context);
        }
    }

    @Override
    public int getDefferedOrder(ParsingContextExecution context) {
        if (deferredOrder > 0) {
            return deferredOrder;
        } else {
            INodeParser<?> innerNodeParser = getParser(context);
            return innerNodeParser.getDefferedOrder(context);
        }
    }

    @Override
    public Object parse(Node node, ParsingContextExecution context) {
        INodeParser<?> innerNodeParser = getParser(context);
        Object innerObject = innerNodeParser.parse(node, context);
        return innerObject;
    }

    private INodeParser<?> getParser(ParsingContextExecution context) {
        INodeParser<?> innerNodeParser = context.getRegistry().get(type);
        if (innerNodeParser == null) {
            context.getParsingErrors().add(new ParsingError(ErrorCode.ALIEN_MAPPING_ERROR, "No parser found for yaml type", null, "", null, type));
        }
        return innerNodeParser;
    }

}