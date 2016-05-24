package alien4cloud.tosca.parser.mapping;

import alien4cloud.tosca.parser.ParsingContextExecution;

public abstract class DefaultDeferredParser<T> extends DefaultParser<T> {

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return true;
    }

}
