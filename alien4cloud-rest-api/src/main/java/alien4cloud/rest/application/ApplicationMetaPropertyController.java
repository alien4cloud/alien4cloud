package alien4cloud.rest.application;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.rest.internal.model.PropertyRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.wordnik.swagger.annotations.Api;

@Slf4j
@RestController
@RequestMapping("/rest/applications/{applicationId:.+}/properties")
@Api(value = "", description = "Operations on Application's meta-properties")
public class ApplicationMetaPropertyController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private MetaPropertiesService metaPropertiesService;

    /**
     * Update or create a property for an application
     *
     * @param applicationId id of the application
     * @param propertyRequest property request
     * @return information on the constraint
     * @throws alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException
     * @throws alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<ConstraintUtil.ConstraintInformation> upsertProperty(@PathVariable String applicationId,
            @RequestBody PropertyRequest propertyRequest)
                    throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        Application application = alienDAO.findById(Application.class, applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        try {
            metaPropertiesService.upsertMetaProperty(application, propertyRequest.getDefinitionId(), propertyRequest.getValue());
        } catch (ConstraintViolationException e) {
            log.error("Constraint violation error for property <" + propertyRequest.getDefinitionId() + "> with value <"
                    + propertyRequest.getValue() + ">", e);
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
            log.error("Constraint value violation error for property <" + e.getConstraintInformation().getName() + "> with value <"
                    + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        }
        return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(null).error(null).build();
    }
}