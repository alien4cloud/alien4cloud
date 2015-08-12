package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.model.components.IndexedInheritableToscaElement;

public class DerivedFromDataTypeParser extends DerivedFromParser {
    public DerivedFromDataTypeParser(Class<? extends IndexedInheritableToscaElement> validType) {
        super(validType);
    }
}
