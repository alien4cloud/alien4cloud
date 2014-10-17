package alien4cloud.tosca.container.deserializer;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

import alien4cloud.tosca.container.exception.CSARTechnicalException;
import alien4cloud.tosca.container.model.type.Operation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase;

/**
 * Utility class to help with complex type deserialization.
 */
public final class ComplexTypeDeserializerHelper {

    private ComplexTypeDeserializerHelper() {
    }

    /**
     * Deserialize a complex object from a given tree node.
     * 
     * @param mapper The object mapper that is used for deserialization.
     * @param node The tree node that contains the object to deserialize.
     * @param objectClass The class of the object to deserialize.
     * @return A deserialized instance of objectClass.
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static <T> T deserializeComplexObject(ObjectMapper mapper, TreeNode node, Class<T> objectClass) throws IllegalArgumentException, IOException {
        try {
            T instance = objectClass.newInstance();
            Iterator<String> nodeIterator = node.fieldNames();
            while (nodeIterator.hasNext()) {
                String childName = nodeIterator.next();
                PropertyDescriptor[] propertyDescriptors;
                propertyDescriptors = Introspector.getBeanInfo(instance.getClass()).getPropertyDescriptors();
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    PropertyNamingStrategy namingStrategy = mapper.getDeserializationConfig().getPropertyNamingStrategy();
                    String propertyName = propertyDescriptor.getName();
                    if (namingStrategy instanceof PropertyNamingStrategyBase) {
                        propertyName = ((PropertyNamingStrategyBase) namingStrategy).translate(propertyName);
                    }
                    if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null && propertyName.equals(childName)) {

                        if (Map.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                            JsonParser parser = mapper.treeAsTokens(node.get(childName));
                            ParameterizedType parameterizedType = (ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType();
                            Type[] types = parameterizedType.getActualTypeArguments();
                            Class<?> keyClass = (Class<?>) types[0];
                            Class<?> valueClass = (Class<?>) types[1];
                            JavaType mapType = mapper.getTypeFactory().constructParametricType(Map.class, keyClass, valueClass);
                            propertyDescriptor.getWriteMethod().invoke(instance, mapper.readValue(parser, mapType));
                        } else {
                            propertyDescriptor.getWriteMethod().invoke(instance,
                                    mapper.treeToValue(node.get(propertyName), propertyDescriptor.getPropertyType()));
                        }
                    }
                }
            }
            return instance;
        } catch (InstantiationException | IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            throw new CSARTechnicalException("Unable to deserialize [" + Operation.class.getName() + "]", e);
        }
    }
}