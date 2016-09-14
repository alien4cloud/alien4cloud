package alien4cloud.utils;

import javax.inject.Inject;

import alien4cloud.tosca.properties.constraints.exception.ConstraintFunctionalException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Rest service that creates a proper REST error from constraint validation.
 */
@Slf4j
@Component
public class RestConstraintValidator {
    @Inject
    private ConstraintPropertyService constraintPropertyService;

    /**
     * Performs validation of a property value against it's definition and eventually build a valid rest error in case of violations.
     *
     * @param propertyName The name of the property to check.
     * @param propertyValue The value of the property to check.
     * @param propertyDefinition The definition of the property.
     * @return Null if no constraints are violated, a RestResponse with validation results in case some constraints are violated.
     */
    public RestResponse<ConstraintUtil.ConstraintInformation> validate(final String propertyName, final Object propertyValue,
            final PropertyDefinition propertyDefinition) {
        if (propertyValue == null || !(propertyValue instanceof String)) {
            // by convention updateproperty with null value => reset to default if exists
            return null;
        }
        try {
            constraintPropertyService.checkSimplePropertyConstraint(propertyName, (String) propertyValue, propertyDefinition);
        } catch (ConstraintFunctionalException e) {
            return fromException(e, propertyName, propertyValue);
        }
        return null;
    }

    public static RestResponse<ConstraintUtil.ConstraintInformation> fromException(ConstraintFunctionalException ex, String propertyName,
            Object propertyValue) {
        if (ex instanceof ConstraintViolationException) {
            ConstraintViolationException e = (ConstraintViolationException) ex;
            log.debug("Constraint violation error for property <" + propertyName + "> with value <" + propertyValue + ">", e);
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } else if (ex instanceof ConstraintValueDoNotMatchPropertyTypeException) {
            ConstraintValueDoNotMatchPropertyTypeException e = (ConstraintValueDoNotMatchPropertyTypeException) ex;
            log.debug("Constraint value violation error for property <" + e.getConstraintInformation().getName() + "> with value <"
                    + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } else {
            throw new IllegalArgumentException("Unexpected ConstraintFunctionalException type", ex);
        }
    }
}