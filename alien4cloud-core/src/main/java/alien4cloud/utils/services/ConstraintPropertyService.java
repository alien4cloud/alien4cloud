package alien4cloud.utils.services;

import java.beans.IntrospectionException;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintTechnicalException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.InvalidVersionException;

/**
 * Common property constraint utils
 * 
 * @author mourouvi
 *
 */
// FIXME: why is this a service ? It looks like a util (with static methods) rather than a service !
@Slf4j
@Service
public class ConstraintPropertyService {

    @Resource
    private ICSARRepositorySearchService csarService;

    /**
     * Check constraints defined on a property for a specified value
     * 
     * @param propertyName Property name (mainly used to create a comprehensive error message)
     * @param stringValue Tested property value
     * @param propertyDefinition Full property definition with type, constraints, default value,...
     * @throws ConstraintViolationException
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     */
    public void checkPropertyConstraint(final String propertyName, final String stringValue, final PropertyDefinition propertyDefinition)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        ConstraintInformation consInformation = null;
        if (propertyDefinition.getConstraints() != null && !propertyDefinition.getConstraints().isEmpty()) {
            for (PropertyConstraint constraint : propertyDefinition.getConstraints()) {
                IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinition.getType());
                try {
                    consInformation = ConstraintUtil.getConstraintInformation(constraint);
                    constraint.initialize(toscaType);
                    constraint.validate(toscaType, stringValue);
                } catch (ConstraintViolationException e) {
                    throw new ConstraintViolationException(e.getMessage(), e, consInformation);
                } catch (IntrospectionException | ConstraintValueDoNotMatchPropertyTypeException e) {
                    // ConstraintValueDoNotMatchPropertyTypeException is not supposed to be raised here (only in constraint definition validation)
                    log.error("Constraint violation introspection error for property <" + propertyName + "> with value <" + stringValue + ">", e);
                    throw new ConstraintTechnicalException("Unable to get constraint informations for property <" + propertyName + ">");
                }
            }
        } else {
            // check any property definition without constraints (type/value)
            try {
                checkBasicType(propertyDefinition, propertyName, stringValue);
            } catch (NumberFormatException | InvalidVersionException e) {
                log.error("Basic type check failed", e);
                consInformation = new ConstraintInformation(propertyName, null, stringValue, propertyDefinition.getType());
                throw new ConstraintValueDoNotMatchPropertyTypeException(e.getMessage(), e, consInformation);
            }
        }
    }

    /**
     *
     * @param propertyName
     * @param complexPropertyValue
     * @param propertyDefinition
     * @param archive
     * @throws ConstraintViolationException
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     */
    public void checkComplexPropertyConstraint(String propertyName, Map<String, Object> complexPropertyValue, PropertyDefinition propertyDefinition,
            ArchiveRoot archive) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        IndexedDataType dataType = ToscaParsingUtil.getDataTypeFromArchiveOrDependencies(propertyDefinition.getType(), archive, csarService);
        for (Map.Entry<String, Object> complexPropertyValueEntry : complexPropertyValue.entrySet()) {
            if (dataType.getProperties() == null || !dataType.getProperties().containsKey(complexPropertyValueEntry.getKey())) {
                throw new ConstraintViolationException("Complex type " + propertyDefinition.getType() + " do not have nested property with name "
                        + complexPropertyValueEntry.getKey() + " for property " + propertyName);
            } else {
                Object nestedPropertyValue = complexPropertyValueEntry.getValue();
                PropertyDefinition nestedPropertyDefinition = dataType.getProperties().get(complexPropertyValueEntry.getKey());
                if (nestedPropertyValue instanceof String) {
                    checkPropertyConstraint(complexPropertyValueEntry.getKey(), (String) nestedPropertyValue, nestedPropertyDefinition);
                } else {
                    checkComplexPropertyConstraint(complexPropertyValueEntry.getKey(), (Map<String, Object>) nestedPropertyValue, nestedPropertyDefinition,
                            archive);
                }
            }
        }
    }

    /**
     * Test type/value regardless constraints
     * 
     * @param propertyDefinition
     * @param propertyValue
     * @throws Exception
     */
    private void checkBasicType(final PropertyDefinition propertyDefinition, final String propertyName, final String propertyValue) {

        // check basic type value : "boolean" (not handled, no exception thrown)
        // "string" (basic case, no exception), "float", "integer", "version"
        String type = propertyDefinition.getType();
        try {
            switch (type) {
            case "integer":
                Integer.parseInt(propertyValue);
                break;
            case "float":
                Float.parseFloat(propertyValue);
                break;
            case "version":
                VersionUtil.parseVersion(propertyValue);
                break;
            default:
                // last type "string" can have any format
                log.info("Type {} not checked yet", type);
                break;
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Float or Integer type invalid check for property [ " + propertyName + " ] and value [ " + propertyValue + " ]");
        } catch (InvalidVersionException e) {
            throw new InvalidVersionException("Version type invalid check for property [ " + propertyName + " ] and value [ " + propertyValue + " ]");
        }
    }

}
