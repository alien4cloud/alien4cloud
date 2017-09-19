package alien4cloud.variable;

import com.google.common.collect.Maps;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.normative.types.ToscaTypes;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ToscaTypeConverter {
    // example of datatype complex:
    // https://github.com/alien4cloud/samples/blob/master/aws-ansible-custom-resources/types.yml
    // https://github.com/alien4cloud/samples/blob/master/demo-lifecycle/demo-lifecycle.yml
    public PropertyValue convert(Object resolvedPropertyValue, PropertyDefinition propertyDefinition) {
        if (resolvedPropertyValue == null) {
            return null;
        }

        if (ToscaTypes.isSimple(propertyDefinition.getType())) {
            return new ScalarPropertyValue((String) resolvedPropertyValue);
        }

        switch (propertyDefinition.getType()) {
        case ToscaTypes.MAP:
            Map<String, Object> map = (Map<String, Object>) resolvedPropertyValue;
            Map<String, Object> finalMap = Maps.newHashMap();
            map.forEach((key, value) -> finalMap.put(key, convert(value, propertyDefinition.getEntrySchema())));
            return new ComplexPropertyValue(finalMap);

        case ToscaTypes.LIST:
            List list = (List) resolvedPropertyValue;
            List finalList = new LinkedList();
            for (Object item : list) {
                finalList.add(convert(item, propertyDefinition.getEntrySchema()));
            }
            return new ListPropertyValue(finalList);

        default:
            // complex type
            // get data type
            // for each map
            // call convert(...) recursively
            // ComplexPropertyValue ?

            throw new IllegalStateException("Property with type <" + propertyDefinition.getType() + "> is not supported");
        }

    }
}
