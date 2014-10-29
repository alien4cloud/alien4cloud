package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.tosca.parser.TypeNodeParser;
import alien4cloud.tosca.parser.TypeReferenceParserFactory;

public abstract class Wd03InheritableToscaElement<T extends IndexedInheritableToscaElement> extends Wd03ToscaElement<T> {
    @Resource
    private TypeReferenceParserFactory typeReferenceParserFactory;
    @Resource
    private Wd03PropertyDefinition propertyDefinition;
    private Class<T> elementClass;

    public Wd03InheritableToscaElement(TypeNodeParser<T> instance, Class<T> elementClass) {
        super(instance);
        this.elementClass = elementClass;
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(typeReferenceParserFactory.getDerivedFromParser(elementClass), "derivedFrom");
        quickMap(propertyDefinition.getParser(), "properties");
    }
}