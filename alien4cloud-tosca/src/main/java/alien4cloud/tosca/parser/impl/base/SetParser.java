package alien4cloud.tosca.parser.impl.base;

import java.util.Collection;

import org.elasticsearch.common.collect.Sets;

import alien4cloud.tosca.parser.INodeParser;

public class SetParser<T> extends CollectionParser<T> {

    public SetParser(INodeParser<T> valueParser, String toscaType) {
        super(valueParser, toscaType, null);
    }

    public SetParser(INodeParser<T> valueParser, String toscaType, String keyPath) {
        super(valueParser, toscaType, keyPath);
    }

    @Override
    protected Collection<T> getCollectionInstance() {
        return Sets.newHashSet();
    }
}