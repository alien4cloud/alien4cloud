package alien4cloud.tosca.parser.mapping;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;

public abstract class DefaultParser<T> implements INodeParser<T> {

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return false;
    }

    @Override
    public int getDeferredOrder(ParsingContextExecution context) {
        return 0;
    }

}
