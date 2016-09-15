package alien4cloud.json.deserializer;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.InRangeConstraint;
import alien4cloud.tosca.properties.constraints.exception.InvalidPropertyConstraintImplementationException;
import alien4cloud.utils.TypeScanner;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

public class PropertyConstraintDeserializer extends StdDeserializer<PropertyConstraint> {

    private static final String CONSTRAINT_PACKAGE = "org.alien4cloud.tosca.model.definitions.constraints";

    private static final long serialVersionUID = 1L;

    private Map<String, Class<? extends PropertyConstraint>> constraints;

    // It's a little bit scary to see something like that ?
    /**
     * A cache which map the naming strategy to the mapping of constraint key to constraint class. It's when a specific naming strategy is defined on our
     * desserialization config.
     */
    private Map<PropertyNamingStrategy, Map<String, Class<? extends PropertyConstraint>>> constraintsCache;

    @SuppressWarnings("unchecked")
    public PropertyConstraintDeserializer() throws IOException, ClassNotFoundException, IntrospectionException {
        super(PropertyConstraint.class);
        this.constraints = Maps.newHashMap();
        this.constraintsCache = Maps.newConcurrentMap();

        // Retrieve all classes which belong to this package
        Set<Class<?>> classes = TypeScanner.scanTypes(CONSTRAINT_PACKAGE, PropertyConstraint.class);

        for (Class<?> propClass : classes) {
            if (!Modifier.isAbstract(propClass.getModifiers())) {
                // Use property as jackson use property
                PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(propClass).getPropertyDescriptors();
                String constraintName = null;
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                        constraintName = propertyDescriptor.getName();
                        break;
                    }
                }
                if (constraintName != null && !constraintName.isEmpty()) {
                    this.constraints.put(constraintName, (Class<? extends PropertyConstraint>) propClass);
                } else {
                    // First field name is also constraint name
                    throw new InvalidPropertyConstraintImplementationException(
                            "A constraint validator must have a readable/writable property to inject configuration.");
                }
            }
        }
    }

    @Override
    public PropertyConstraint deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        ObjectNode node = mapper.readTree(jp);
        String constraintName = null;
        Iterator<String> fieldIterator = node.fieldNames();
        // First field is also the constraint name ?
        if (fieldIterator.hasNext()) {
            constraintName = fieldIterator.next();
        } else {
            throw JsonMappingException.from(jp, "Constraint definition must contain one field");
        }
        PropertyNamingStrategy namingStrategy = mapper.getDeserializationConfig().getPropertyNamingStrategy();
        Map<String, Class<? extends PropertyConstraint>> constraintsMapping = getTranslatedConstraintsMap(namingStrategy);
        if (!constraintsMapping.containsKey(constraintName)) {
            if ("rangeMinValue".equals(constraintName) || "rangeMaxValue".equals(constraintName)) {
                return mapper.treeToValue(node, InRangeConstraint.class);
            } else {
                throw JsonMappingException.from(jp, "Constraint not found [" + constraintName + "], expect one of [" + this.constraints.keySet() + "]");
            }
        }
        Class<? extends PropertyConstraint> constraintClass = constraintsMapping.get(constraintName);
        return mapper.treeToValue(node, constraintClass);
    }

    private Map<String, Class<? extends PropertyConstraint>> getTranslatedConstraintsMap(PropertyNamingStrategy namingStrategy) {
        if (namingStrategy instanceof PropertyNamingStrategyBase) {
            Map<String, Class<? extends PropertyConstraint>> cachedConstraints = constraintsCache.get(namingStrategy);
            if (cachedConstraints != null) {
                return cachedConstraints;
            } else {
                Map<String, Class<? extends PropertyConstraint>> translatedConstraints = Maps.newHashMap();
                for (Map.Entry<String, Class<? extends PropertyConstraint>> entry : this.constraints.entrySet()) {
                    translatedConstraints.put(((PropertyNamingStrategyBase) namingStrategy).translate(entry.getKey()), entry.getValue());
                }
                constraintsCache.put(namingStrategy, translatedConstraints);
                return translatedConstraints;
            }
        } else {
            return this.constraints;
        }
    }
}
