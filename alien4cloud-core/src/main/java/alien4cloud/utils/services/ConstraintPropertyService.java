package alien4cloud.utils.services;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.InvalidArgumentException;
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
@Slf4j
@Service
public class ConstraintPropertyService {

    @Resource
    private ICSARRepositorySearchService searchService;

    public void checkPropertyConstraint(String propertyName, Object propertyValue, PropertyDefinition propertyDefinition, ArchiveRoot archive)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        if (propertyValue instanceof String) {
            checkSimplePropertyConstraint(propertyName, (String) propertyValue, propertyDefinition);
        } else if (propertyValue instanceof Map) {
            checkComplexPropertyConstraint(propertyName, (Map<String, Object>) propertyValue, propertyDefinition, archive);
        } else if (propertyValue instanceof List) {
            checkListPropertyConstraint(propertyName, (List<Object>) propertyValue, propertyDefinition, archive);
        } else {
            throw new InvalidArgumentException("Not expecting to receive constraint validation for other types than String, Map or List as "
                    + propertyValue.getClass().getName());
        }
    }

    /**
     * Check constraints defined on a property for a specified value
     * 
     * @param propertyName Property name (mainly used to create a comprehensive error message)
     * @param stringValue Tested property value
     * @param propertyDefinition Full property definition with type, constraints, default value,...
     * @throws ConstraintViolationException
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     */
    public void checkSimplePropertyConstraint(final String propertyName, final String stringValue, final PropertyDefinition propertyDefinition)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        ConstraintInformation consInformation = null;
        if (propertyDefinition.getConstraints() != null && !propertyDefinition.getConstraints().isEmpty()) {
            for (PropertyConstraint constraint : propertyDefinition.getConstraints()) {
                IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinition.getType());
                try {
                    consInformation = ConstraintUtil.getConstraintInformation(constraint);
                    consInformation.setPath(propertyName + ".constraints[" + consInformation.getName() + "]");
                    constraint.initialize(toscaType);
                    constraint.validate(toscaType, stringValue);
                } catch (ConstraintViolationException e) {
                    throw new ConstraintViolationException(e.getMessage(), e, consInformation);
                } catch (IntrospectionException e) {
                    // ConstraintValueDoNotMatchPropertyTypeException is not supposed to be raised here (only in constraint definition validation)
                    log.error("Constraint introspection error for property <" + propertyName + "> value <" + stringValue + ">", e);
                    throw new ConstraintTechnicalException("Constraint introspection error for property <" + propertyName + "> value <"
                            + stringValue + ">", e);
                }
            }
        } else {
            // check any property definition without constraints (type/value)
            try {
                checkBasicType(propertyName, propertyDefinition, stringValue);
            } catch (NumberFormatException | InvalidVersionException e) {
                log.error("Basic type check failed", e);
                consInformation = new ConstraintInformation(propertyName, null, stringValue, propertyDefinition.getType());
                throw new ConstraintValueDoNotMatchPropertyTypeException(e.getMessage(), e, consInformation);
            }
        }
    }

    public void checkDataTypePropertyConstraint(String propertyName, Map<String, Object> complexPropertyValue, PropertyDefinition propertyDefinition,
            ArchiveRoot archive) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        IndexedDataType dataType = ToscaParsingUtil.getDataTypeFromArchiveOrDependencies(propertyDefinition.getType(), archive, searchService);
        for (Map.Entry<String, Object> complexPropertyValueEntry : complexPropertyValue.entrySet()) {
            if (dataType.getProperties() == null || !dataType.getProperties().containsKey(complexPropertyValueEntry.getKey())) {
                throw new ConstraintViolationException("Complex type " + propertyDefinition.getType() + " do not have nested property with name "
                        + complexPropertyValueEntry.getKey() + " for property " + propertyName);
            } else {
                Object nestedPropertyValue = complexPropertyValueEntry.getValue();
                PropertyDefinition nestedPropertyDefinition = dataType.getProperties().get(complexPropertyValueEntry.getKey());
                checkPropertyConstraint(propertyName + "." + complexPropertyValueEntry.getKey(), nestedPropertyValue, nestedPropertyDefinition, archive);
            }
        }
    }

    public void checkListPropertyConstraint(String propertyName, List<Object> listPropertyValue, PropertyDefinition propertyDefinition,
            ArchiveRoot archive) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        PropertyDefinition entrySchema = propertyDefinition.getEntrySchema();
        for (int i = 0; i < listPropertyValue.size(); i++) {
            checkPropertyConstraint(propertyName + "[" + String.valueOf(i) + "]", listPropertyValue.get(i), entrySchema, archive);
        }
    }

    public void checkMapPropertyConstraint(String propertyName, Map<String, Object> complexPropertyValue, PropertyDefinition propertyDefinition,
            ArchiveRoot archive) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        PropertyDefinition entrySchema = propertyDefinition.getEntrySchema();
        for (Map.Entry<String, Object> complexPropertyValueEntry : complexPropertyValue.entrySet()) {
            checkPropertyConstraint(propertyName + "." + complexPropertyValueEntry.getKey(), complexPropertyValueEntry.getValue(), entrySchema, archive);
        }
    }

    /**
     * Verify that a complex property value correspond to its definition of constraints
     * 
     * @param propertyName name of the property
     * @param complexPropertyValue the value
     * @param propertyDefinition the definition
     * @param archive the archive containing the definition
     * @throws ConstraintViolationException
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     */
    public void checkComplexPropertyConstraint(String propertyName, Map<String, Object> complexPropertyValue, PropertyDefinition propertyDefinition,
            ArchiveRoot archive) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        if (ToscaType.MAP.equals(propertyDefinition.getType())) {
            checkMapPropertyConstraint(propertyName, complexPropertyValue, propertyDefinition, archive);
        } else {
            checkDataTypePropertyConstraint(propertyName, complexPropertyValue, propertyDefinition, archive);
        }
    }

    /**
     * Test type/value regardless constraints
     * 
     * @param propertyDefinition
     * @param propertyValue
     * @throws Exception
     */
    private void checkBasicType(final String propertyName, final PropertyDefinition propertyDefinition, final String propertyValue) {

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
                break;
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(propertyName + "'s value is not a number [ " + propertyValue + " ]");
        } catch (InvalidVersionException e) {
            throw new InvalidVersionException(propertyName + "'s value is not a version [ " + propertyValue + " ]");
        }
    }

}
