package alien4cloud.rest.internal;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
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
 *
 * @author mourouvi
 *
 */

@Slf4j
@RestController
@RequestMapping("/rest/properties")
public class PropertiesController {

    @Resource
    private ConstraintPropertyService constraintPropertyService;

    @RequestMapping(value = "/check", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ConstraintInformation> checkPropertyDefinition(@RequestBody PropertyRequest propertyRequest) {

        if (propertyRequest.getPropertyDefinition() != null) {
            try {
                constraintPropertyService.checkPropertyConstraint(propertyRequest.getPropertyId(), propertyRequest.getPropertyValue(),
                        propertyRequest.getPropertyDefinition());
            } catch (ConstraintViolationException e) {
                log.error("Constraint violation error for property <" + propertyRequest.getPropertyId() + "> with value <" + propertyRequest.getPropertyValue()
                        + ">", e);
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
