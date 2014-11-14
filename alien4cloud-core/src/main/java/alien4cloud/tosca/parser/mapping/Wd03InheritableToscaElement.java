package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;
import javax.validation.Validator;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.tosca.parser.impl.advanced.TypeReferenceParserFactory;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;
import alien4cloud.tosca.parser.impl.base.ValidatedNodeParser;

public abstract class Wd03InheritableToscaElement<T extends IndexedInheritableToscaElement> extends Wd03ToscaElement<T> {
    @Resource
    private TypeReferenceParserFactory typeReferenceParserFactory;
    @Resource
    private Wd03PropertyDefinition propertyDefinition;
    @Resource
    private Validator validator;

    private Class<T> elementClass;

    public Wd03InheritableToscaElement(TypeNodeParser<T> instance, Class<T> elementClass) {
        super(instance);
        this.elementClass = elementClass;
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(typeReferenceParserFactory.getDerivedFromParser(elementClass), "derivedFrom");
        quickMap(new MapParser<PropertyDefinition>(new ValidatedNodeParser<PropertyDefinition>(validator, propertyDefinition.getParser()), "Properties"),
                "properties");
    }
}