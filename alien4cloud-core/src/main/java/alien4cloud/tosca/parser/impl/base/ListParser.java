package alien4cloud.tosca.parser.impl.base;

import java.util.Collection;

import alien4cloud.tosca.parser.INodeParser;

import com.google.common.collect.Lists;

public class ListParser<T> extends CollectionParser<T> {

    public ListParser(INodeParser<T> valueParser, String toscaType) {
        super(valueParser, toscaType, null);
    }

    public ListParser(INodeParser<T> valueParser, String toscaType, String keyPath) {
        super(valueParser, toscaType, keyPath);
    }

    @Override
    protected Collection<T> getCollectionInstance() {
        return Lists.newArrayList();
    }
}