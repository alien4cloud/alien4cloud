package alien4cloud.tosca.parser.mapping;

import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.tosca.parser.TypeNodeParser;

public abstract class Wd03ToscaElement<T extends IndexedToscaElement> extends AbstractMapper<T> {
    public Wd03ToscaElement(TypeNodeParser<T> instance) {
        super(instance);
    }

    @Override
    public void initMapping() {
        quickMap("description");
        quickMap("tags");
    }
}