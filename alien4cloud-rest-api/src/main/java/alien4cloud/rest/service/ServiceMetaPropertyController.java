package alien4cloud.rest.service;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.rest.common.AbstractMetaPropertyController;
import alien4cloud.rest.internal.model.PropertyRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.inject.Inject;

@Slf4j
@RestController
@RequestMapping({"/rest/services/{id:.+}/properties", "/rest/v1/services/{id:.+}/properties", "/rest/latest/services/{id:.+}/properties"})
@Api(value = "", description = "Operations on Service's meta-properties")
public class ServiceMetaPropertyController extends AbstractMetaPropertyController {

    @Resource
    private ServiceResourceService serviceResourceService;

    /**
     * Update or create a property for an application
     *
     * @param id id of the service
     * @param propertyRequest property request
     * @return information on the constraint
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     * @throws ConstraintViolationException
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<ConstraintUtil.ConstraintInformation> upsertProperty(@PathVariable String id,
            @RequestBody PropertyRequest propertyRequest)
                    throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        return super.upsertProperty(id, propertyRequest);
    }

    protected IMetaProperties getTarget(String id) {
        return serviceResourceService.getOrFail(id);
    }

}