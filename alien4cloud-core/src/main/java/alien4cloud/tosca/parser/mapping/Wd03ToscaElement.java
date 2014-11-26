package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.tosca.parser.impl.advanced.TagParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

public abstract class Wd03ToscaElement<T extends IndexedToscaElement> extends AbstractMapper<T> {
    @Resource
    private TagParser tagParser;

    public Wd03ToscaElement(TypeNodeParser<T> instance) {
        super(instance);
    }

    @Override
    public void initMapping() {
        quickMap("description");
        quickMap(tagParser, "tags");
    }
}