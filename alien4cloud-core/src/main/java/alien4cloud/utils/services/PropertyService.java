package alien4cloud.utils.services;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.exception.InvalidArgumentException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

/**
 * Service to set and check constraints on properties.
 */
@Service
public class PropertyService {
    @Inject
    private ConstraintPropertyService constraintPropertyService;

    public <T extends AbstractPropertyValue> void setPropertyValue(Map<String, T> properties, PropertyDefinition propertyDefinition, String propertyName,
            Object propertyValue) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        // take the default value
        if (propertyValue == null) {
            // no check here, the default value has to be valid at parse time
            properties.put(propertyName, (T) propertyDefinition.getDefault());
            return;
        }

        // if the default value is also empty, we set the property value to null
        constraintPropertyService.checkPropertyConstraint(propertyName, propertyValue, propertyDefinition);
        if (propertyValue instanceof String) {
            properties.put(propertyName, (T) new ScalarPropertyValue((String) propertyValue));
        } else if (propertyValue instanceof Map) {
            properties.put(propertyName, (T) new ComplexPropertyValue((Map<String, Object>) propertyValue));
        } else if (propertyValue instanceof List) {
            properties.put(propertyName, (T) new ListPropertyValue((List<Object>) propertyValue));
        } else {
            throw new InvalidArgumentException("Property type " + propertyValue.getClass().getName() + " is invalid");
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
     * Set value for a property
     * 
     * @param dependencies all tosca dependencies for current operation
     * @param nodeTemplate the node template
     * @param propertyDefinition the definition of the property to be set
     * @param propertyName the name of the property to set
     * @param propertyValue the value to be set
     */
    @ToscaContextual
    public void setPropertyValue(Set<CSARDependency> dependencies, NodeTemplate nodeTemplate, PropertyDefinition propertyDefinition, String propertyName,
            Object propertyValue) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        setPropertyValue(nodeTemplate, propertyDefinition, propertyName, propertyValue);
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
