package alien4cloud.rest.internal;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.internal.model.PropertyValidationRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.utils.services.ConstraintPropertyService;
import lombok.extern.slf4j.Slf4j;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Handle generic operation on "properties"
 */
@Slf4j
@ApiIgnore
@RestController
@RequestMapping({ "/rest/properties", "/rest/v1/properties", "/rest/latest/properties" })
public class PropertiesController {
    @ApiIgnore
    @RequestMapping(value = "/check", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ConstraintInformation> checkPropertyDefinition(@RequestBody PropertyValidationRequest propertyValidationRequest) {
        if (propertyValidationRequest.getPropertyDefinition() != null) {
            try {
                if (propertyValidationRequest.getDependencies() != null) {
                    ToscaContext.init(propertyValidationRequest.getDependencies());
                }
                ConstraintPropertyService.checkPropertyConstraint(propertyValidationRequest.getDefinitionId(), propertyValidationRequest.getValue(),
                        propertyValidationRequest.getPropertyDefinition());
            } catch (ConstraintViolationException e) {
                log.error("Constraint violation error for property <" + propertyValidationRequest.getDefinitionId() + "> with value <"
                        + propertyValidationRequest.getValue() + ">", e);
                return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {

                ConstraintInformation constraintInformation = e.getConstraintInformation() != null ? e.getConstraintInformation()
                        : new ConstraintInformation(propertyValidationRequest.getDefinitionId(), null, propertyValidationRequest.getValue().toString(),
                                propertyValidationRequest.getPropertyDefinition().getType());

                log.error("Constraint value violation error for property <" + constraintInformation.getName() + "> with value <"
                        + constraintInformation.getValue() + "> and type <" + constraintInformation.getType() + ">", e);
                return RestResponseBuilder.<ConstraintInformation> builder().data(constraintInformation)
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            } finally {
                if (ToscaContext.get() != null) {
                    ToscaContext.destroy();
                }
            }
        }

        return RestResponseBuilder.<ConstraintInformation> builder().build();
    }
}