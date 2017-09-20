package alien4cloud.variable;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.PrimitiveDataType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.alien4cloud.tosca.utils.DataTypesFetcher;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ToscaTypeConverter {

    private DataTypesFetcher.DataTypeFinder dataTypeFinder;

    public ToscaTypeConverter(DataTypesFetcher.DataTypeFinder dataTypeFinder) {
        this.dataTypeFinder = dataTypeFinder;
    }

    public Object toValue(Object resolvedPropertyValue, PropertyDefinition propertyDefinition) {
        if (resolvedPropertyValue == null) {
            return null;
        }

        if (ToscaTypes.isSimple(propertyDefinition.getType())) {
            try {
                return ToscaTypes.fromYamlTypeName(propertyDefinition.getType()).parse(resolvedPropertyValue.toString());
            } catch (InvalidPropertyValueException e) {
                throw new RuntimeException("failed to parse <" + resolvedPropertyValue + "> as type <" + propertyDefinition.getType() + ">", e);
            }
        }

        switch (propertyDefinition.getType()) {
            case ToscaTypes.MAP:
                if (resolvedPropertyValue instanceof Map) {
                    return resolvedPropertyValue;
                } else {
                    throw new IllegalStateException("Property is not the expected type <" + Map.class.getSimpleName() + "> but was <" + resolvedPropertyValue.getClass().getName() + ">");
                }

            case ToscaTypes.LIST:
                if (resolvedPropertyValue instanceof Collection) {
                    return resolvedPropertyValue;
                } else {
                    throw new IllegalStateException("Property is not the expected type <" + Collection.class.getSimpleName() + "> but was <" + resolvedPropertyValue.getClass().getName() + ">");
                }

            default:
                DataType dataType = findDataType(propertyDefinition.getType());
                if (dataType == null) {
                    throw new IllegalStateException("Property with type <" + propertyDefinition.getType() + "> is not supported");
                }

                if (dataType.isDeriveFromSimpleType()) {
                    return new ScalarPropertyValue((String) resolvedPropertyValue);
                } else if (resolvedPropertyValue instanceof Map) {
                    Map<String, Object> complexPropertyValue = (Map<String, Object>) resolvedPropertyValue;
                    Map<String, Object> finalComplex = Maps.newHashMap();
                    for (Map.Entry<String, Object> complexPropertyValueEntry : complexPropertyValue.entrySet()) {
                        Object nestedPropertyValue = complexPropertyValueEntry.getValue();
                        PropertyDefinition nestedPropertyDefinition = dataType.getProperties().get(complexPropertyValueEntry.getKey());
                        finalComplex.put(complexPropertyValueEntry.getKey(), toPropertyValue(nestedPropertyValue, nestedPropertyDefinition));
                    }
                    return new ComplexPropertyValue(finalComplex);
                } else {
                    throw new IllegalStateException("Property with type <" + propertyDefinition.getType() + "> is not supported.");
                }

        }

    }

    // example of datatype complex:
    // https://github.com/alien4cloud/samples/blob/master/aws-ansible-custom-resources/types.yml
    // https://github.com/alien4cloud/samples/blob/master/demo-lifecycle/demo-lifecycle.yml
    public PropertyValue toPropertyValue(Object resolvedPropertyValue, PropertyDefinition propertyDefinition) {
        if (resolvedPropertyValue == null) {
            return null;
        }

        if (ToscaTypes.isSimple(propertyDefinition.getType())) {
            return new ScalarPropertyValue((String) resolvedPropertyValue);
        }

        switch (propertyDefinition.getType()) {
            case ToscaTypes.MAP:
                if (resolvedPropertyValue instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) resolvedPropertyValue;
                    Map<String, Object> finalMap = Maps.newHashMap();
                    map.forEach((key, value) -> finalMap.put(key, toPropertyValue(value, propertyDefinition.getEntrySchema())));
                    return new ComplexPropertyValue(finalMap);
                } else {
                    throw new IllegalStateException("Property is not the expected type <" + Map.class.getSimpleName() + "> but was <" + resolvedPropertyValue.getClass().getName() + ">");
                }

            case ToscaTypes.LIST:
                if (resolvedPropertyValue instanceof Collection) {
                    List list = (List) resolvedPropertyValue;
                    List finalList = new LinkedList();
                    for (Object item : list) {
                        finalList.add(toPropertyValue(item, propertyDefinition.getEntrySchema()));
                    }
                    return new ListPropertyValue(finalList);
                } else {
                    throw new IllegalStateException("Property is in the expected type <" + Collection.class.getSimpleName() + "> but was <" + resolvedPropertyValue.getClass().getName() + ">");
                }

            default:
                DataType dataType = findDataType(propertyDefinition.getType());

                if (dataType == null) {
                    throw new IllegalStateException("Property with type <" + propertyDefinition.getType() + "> is not supported");
                }

                if (dataType.isDeriveFromSimpleType()) {
                    return new ScalarPropertyValue((String) resolvedPropertyValue);
                } else if (resolvedPropertyValue instanceof Map) {
                    return new ComplexPropertyValue((Map<String, Object>) resolvedPropertyValue);
                } else {
                    throw new IllegalStateException("Property with type <" + propertyDefinition.getType() + "> is not supported.");
                }
        }

    }

    private DataType findDataType(String type) {
        DataType dataType = dataTypeFinder.findDataType(DataType.class, type);
        if (dataType == null) {
            dataType = dataTypeFinder.findDataType(PrimitiveDataType.class, type);
        }
        return dataType;
    }
}
