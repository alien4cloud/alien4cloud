package alien4cloud.rest.internal;

import javax.annotation.Resource;

import alien4cloud.rest.internal.model.PropertyValidationRequest;
import springfox.documentation.annotations.ApiIgnore;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Handle generic operation on "properties"
 */
@Slf4j
@ApiIgnore
@RestController
@RequestMapping({"/rest/properties", "/rest/v1/properties", "/rest/latest/properties"})
public class PropertiesController {
    @Resource
    private ConstraintPropertyService constraintPropertyService;

    @ApiIgnore
    @RequestMapping(value = "/check", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'APPLICATIONS_MANAGER')")
    public RestResponse<ConstraintInformation> checkPropertyDefinition(@RequestBody PropertyValidationRequest propertyValidationRequest) {
        if (propertyValidationRequest.getPropertyDefinition() != null) {
            try {
                constraintPropertyService.checkSimplePropertyConstraint(propertyValidationRequest.getDefinitionId(), propertyValidationRequest.getValue(),
                        propertyValidationRequest.getPropertyDefinition());
            } catch (ConstraintViolationException e) {
                log.error("Constraint violation error for property <" + propertyValidationRequest.getDefinitionId() + "> with value <"
                        + propertyValidationRequest.getValue() + ">", e);
                return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                log.error("Constraint value violation error for property <" + e.getConstraintInformation().getName() + "> with value <"
                        + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
                return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            }
        }

        return RestResponseBuilder.<ConstraintInformation> builder().build();
    }
}