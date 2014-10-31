package alien4cloud.tosca.parser;

import java.util.Collection;

import org.elasticsearch.common.collect.Sets;

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