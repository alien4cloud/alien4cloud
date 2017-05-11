package org.alien4cloud.tosca.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.AttributeDefinition;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.PrimitiveDataType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;

public class DataTypesFetcher {

    public interface DataTypeFinder {
        DataType findDataType(Class<? extends DataType> concreteType, String id);
    }

    /**
     * Get all data types that the given inheritable types depends on (in properties definitions)
     *
     * @param elements the types to search for data types
     * @param <T> the type of inheritable type (node type, data type, capability type ...)
     * @return a map of type id to data type
     */
    public static <T extends AbstractInheritableToscaType> Map<String, DataType> getDataTypesDependencies(Collection<T> elements,
            DataTypeFinder dataTypeFinder) {
        Map<String, DataType> indexedDataTypes = new HashMap<>();
        for (AbstractInheritableToscaType indexedNodeType : elements) {
            if (indexedNodeType != null) {
                if (indexedNodeType.getProperties() != null) {
                    for (PropertyDefinition propertyDefinition : indexedNodeType.getProperties().values()) {
                        getDataTypeDependency(indexedDataTypes, propertyDefinition.getType(), dataTypeFinder);
                    }
                }
                if (indexedNodeType instanceof AbstractInstantiableToscaType && ((AbstractInstantiableToscaType) indexedNodeType).getAttributes() != null) {
                    for (IValue attributeDefinition : ((AbstractInstantiableToscaType) indexedNodeType).getAttributes().values()) {
                        if (attributeDefinition instanceof AttributeDefinition) {
                            getDataTypeDependency(indexedDataTypes, ((AttributeDefinition) attributeDefinition).getType(), dataTypeFinder);
                        }
                    }
                }
            }
        }
        return indexedDataTypes;
    }

    private static void getDataTypeDependency(Map<String, DataType> indexedDataTypes, String type, DataTypeFinder dataTypeFinder) {
        if (ToscaTypes.isPrimitive(type) || indexedDataTypes.containsKey(type)) {
            return;
        }
        DataType dataType = dataTypeFinder.findDataType(DataType.class, type);
        if (dataType == null) {
            dataType = dataTypeFinder.findDataType(PrimitiveDataType.class, type);
        }
        indexedDataTypes.put(type, dataType);
    }
}
