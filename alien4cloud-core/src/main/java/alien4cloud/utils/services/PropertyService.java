package alien4cloud.utils.services;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.tosca.context.ToscaContextual;

/**
 * Service to set and check constraints on properties.
 */
@Service
public class PropertyService {

    public <T extends AbstractPropertyValue> void setPropertyValue(Map<String, T> properties, PropertyDefinition propertyDefinition, String propertyName,
            Object propertyValue) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        // take the default value
        if (propertyValue == null) {
            // no check here, the default value has to be valid at parse time
            properties.put(propertyName, (T) propertyDefinition.getDefault());
            return;
        }

        T abstractPropertyValue = asPropertyValue(propertyValue);
        if (!(abstractPropertyValue instanceof FunctionPropertyValue)) {
            ConstraintPropertyService.checkPropertyConstraint(propertyName, propertyValue, propertyDefinition);
        }

        properties.put(propertyName, abstractPropertyValue);
    }

    public static <T extends AbstractPropertyValue> T asPropertyValue(Object propertyValue) {
        if (propertyValue instanceof PropertyValue) {
            return (T) propertyValue;
        } else if (propertyValue instanceof String) {
            return (T) new ScalarPropertyValue((String) propertyValue);
        } else if (propertyValue instanceof Integer) {
            return (T) new ScalarPropertyValue(((Integer) propertyValue).toString());
        } else if (propertyValue instanceof FunctionPropertyValue) {
            return (T) propertyValue;
        } else if (propertyValue instanceof Map) {
            return asFunctionPropertyValue(propertyValue);
        } else if (propertyValue instanceof List) {
            return (T) new ListPropertyValue((List<Object>) propertyValue);
        } else {
            throw new InvalidArgumentException("Property type " + propertyValue.getClass().getName() + " is invalid");
        }
    }

    public static <T extends AbstractPropertyValue> T asFunctionPropertyValue(Object propertyValue) {
        if (propertyValue instanceof Map) {
            Map valueAsMap = (Map) propertyValue;
            if (valueAsMap.keySet().contains("function") && valueAsMap.keySet().contains("parameters")) {
                if (!ToscaFunctionConstants.GET_SECRET.equals(valueAsMap.get("function"))) {
                    throw new InvalidArgumentException("Property function " + valueAsMap.get("function") + " is invalid");
                }
                FunctionPropertyValue functionPropertyValue = new FunctionPropertyValue();
                functionPropertyValue.setFunction((String) valueAsMap.get("function"));
                functionPropertyValue.setParameters((List<String>) valueAsMap.get("parameters"));
                return (T) functionPropertyValue;
            }
        }
        return (T) new ComplexPropertyValue((Map<String, Object>) propertyValue);
    }

    /**
     * Set value for a property
     *
     * @param template the template
     * @param propertyDefinition the definition of the property to be set
     * @param propertyName the name of the property to set
     * @param propertyValue the value to be set
     */
    public void setPropertyValue(AbstractTemplate template, PropertyDefinition propertyDefinition, String propertyName, Object propertyValue)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        if (template.getProperties() == null) {
            template.setProperties(Maps.newLinkedHashMap());
        }
        setPropertyValue(template.getProperties(), propertyDefinition, propertyName, propertyValue);
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
    public void setPropertyValue(Set<CSARDependency> dependencies, AbstractTemplate nodeTemplate, PropertyDefinition propertyDefinition, String propertyName,
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
            capability.setProperties(Maps.newLinkedHashMap());
        }
        setPropertyValue(capability.getProperties(), propertyDefinition, propertyName, propertyValue);
    }

    /**
     * Set value for a capability property
     *
     * @param capability the capability
     * @param propertyDefinition the definition of the property
     * @param propertyName the name of the property
     * @param propertyValue the value of the property
     */
    @ToscaContextual
    public void setCapabilityPropertyValue(Set<CSARDependency> dependencies, Capability capability, PropertyDefinition propertyDefinition, String propertyName,
            Object propertyValue)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        setCapabilityPropertyValue(capability, propertyDefinition, propertyName, propertyValue);
    }
}
