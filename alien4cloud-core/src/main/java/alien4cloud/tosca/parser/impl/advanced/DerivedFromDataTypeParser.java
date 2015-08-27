package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedDataType;

@Component
public class DerivedFromDataTypeParser extends DerivedFromParser {
    public DerivedFromDataTypeParser() {
        super(IndexedDataType.class);
    }
}
