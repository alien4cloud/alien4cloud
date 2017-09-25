package org.alien4cloud.tosca.variable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.PrimitiveDataType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.alien4cloud.tosca.utils.DataTypesFetcher;

import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * This class can convert {@link Object} into {@link PropertyValue} with respect to a {@link PropertyDefinition}.
 */
@Slf4j
public class ToscaTypeConverter {

    private DataTypesFetcher.DataTypeFinder dataTypeFinder;

    public ToscaTypeConverter(DataTypesFetcher.DataTypeFinder dataTypeFinder) {
        this.dataTypeFinder = dataTypeFinder;
    }

    @SuppressWarnings("unchecked")
    public PropertyValue toPropertyValue(Object resolvedPropertyValue, PropertyDefinition propertyDefinition) {
        if (resolvedPropertyValue == null) {
            return null;
        }

        if (ToscaTypes.isSimple(propertyDefinition.getType())) {
            return new ScalarPropertyValue(resolvedPropertyValue.toString());
        }

        switch (propertyDefinition.getType()) {
            case ToscaTypes.MAP:
                if (resolvedPropertyValue instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) resolvedPropertyValue;
                    Map<String, Object> resultMap = Maps.newHashMap();
                    map.forEach((key, value) -> resultMap.put(key, toPropertyValue(value, propertyDefinition.getEntrySchema())));
                    return new ComplexPropertyValue(resultMap);
                } else {
                    throw new IllegalStateException("Property is not the expected type <" + Map.class.getSimpleName() + "> but was <" + resolvedPropertyValue.getClass().getName() + ">");
                }

            case ToscaTypes.LIST:
                if (resolvedPropertyValue instanceof Collection) {
                    List list = (List) resolvedPropertyValue;
                    List resultList = new LinkedList();
                    for (Object item : list) {
                        resultList.add(toPropertyValue(item, propertyDefinition.getEntrySchema()));
                    }
                    return new ListPropertyValue(resultList);
                } else {
                    throw new IllegalStateException("Property is in the expected type <" + Collection.class.getSimpleName() + "> but was <" + resolvedPropertyValue.getClass().getName() + ">");
                }

            default:
                DataType dataType = findDataType(propertyDefinition.getType());

                if (dataType == null) {
                    throw new IllegalStateException("Property with type <" + propertyDefinition.getType() + "> is not supported");
                }

                if (dataType.isDeriveFromSimpleType()) {
                    return new ScalarPropertyValue(resolvedPropertyValue.toString());
                } else if (resolvedPropertyValue instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) resolvedPropertyValue;
                    /*
                    Map<String, Object> resultMap = Maps.newHashMap();

                    map.forEach((key, value) -> {
                        PropertyDefinition entryDefinition = dataType.getProperties().get(key);
                        if(entryDefinition == null){
                            throw new IllegalStateException("DataType <" + propertyDefinition.getType() + "> does not contains any definition for entry <" + key + ">");
                        }
                        resultMap.put(key, toPropertyValue(value, entryDefinition));
                    });
                    return new ComplexPropertyValue(resultMap);
                    */
                    return new ComplexPropertyValue(map);
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
