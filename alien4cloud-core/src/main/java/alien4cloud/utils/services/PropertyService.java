package alien4cloud.utils.services;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.*;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.google.common.collect.Maps;

/**
 * Service to set and check constraints on properties.
 */
@Service
public class PropertyService {
    @Inject
    private ConstraintPropertyService constraintPropertyService;

    public void setPropertyValue(Map<String, AbstractPropertyValue> properties, PropertyDefinition propertyDefinition, String propertyName,
            Object propertyValue) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        // take the default value
        if (propertyValue == null) {
            propertyValue = propertyDefinition.getDefault();
        }

        // if the default value is also empty, we set the property value to null
        if (propertyValue == null) {
            properties.put(propertyName, null);
        } else {
            if (propertyValue instanceof String) {
                constraintPropertyService.checkSimplePropertyConstraint(propertyName, (String) propertyValue, propertyDefinition);
                properties.put(propertyName, new ScalarPropertyValue((String) propertyValue));
            } else if (propertyValue instanceof Map) {
                // TODO check complex constraints.
                properties.put(propertyName, new ComplexPropertyValue((Map<String, Object>) propertyValue));
            } else if (propertyValue instanceof List) {
                // TODO check complex constraints.
                properties.put(propertyName, new ListPropertyValue((List<Object>) propertyValue));
            } else {
                throw new InvalidArgumentException("Property type " + propertyValue.getClass().getName() + " is invalid");
            }
        }
    }

    /**
     * Set value for a property
     *
     * @param nodeTemplate the node template
     * @param propertyDefinition the definition of the property to be set
     * @param propertyName the name of the property to set
     * @param propertyValue the value to be set
     */
    public void setPropertyValue(NodeTemplate nodeTemplate, PropertyDefinition propertyDefinition, String propertyName, Object propertyValue)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        if (nodeTemplate.getProperties() == null) {
            nodeTemplate.setProperties(Maps.<String, AbstractPropertyValue> newHashMap());
        }
        setPropertyValue(nodeTemplate.getProperties(), propertyDefinition, propertyName, propertyValue);
    }

    /**
     * Set value for a capability property
     *
     * @param capability the capability
     * @param propertyDefinition the definition of the property
     * @param propertyName the name of the property
     * @param propertyValue the value of the property
     */
    public void setCapabilityPropertyValue(Capability capability, PropertyDefinition propertyDefinition, String propertyName, Object propertyValue)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        if (capability.getProperties() == null) {
            capability.setProperties(Maps.<String, AbstractPropertyValue> newHashMap());
        }
        setPropertyValue(capability.getProperties(), propertyDefinition, propertyName, propertyValue);
    }
}
