package alien4cloud.tosca.parser.impl.base;

import java.util.Collection;

import alien4cloud.tosca.parser.INodeParser;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ListParser<T> extends CollectionParser<T> {

    /**
     * Constructor called by spring (see BaseParserFactory).
     * 
     * @param valueParser The parser to use to parse list values.
     * @param toscaType The expected type name to generate error messages.
     */
    public ListParser(INodeParser<T> valueParser, String toscaType) {
        super(valueParser, toscaType, null);
    }

    /**
     * Constructor called by spring (see BaseParserFactory).
     *
     * @param valueParser The parser to use to parse list values.
     * @param toscaType The expected type name to generate error messages.
     * @param keyPath In case the list is created from a map, optional value to inject the key into the value object.
     */
    public ListParser(INodeParser<T> valueParser, String toscaType, String keyPath) {
        super(valueParser, toscaType, keyPath);
    }

    @Override
    protected Collection<T> getCollectionInstance() {
        return Lists.newArrayList();
    }
}