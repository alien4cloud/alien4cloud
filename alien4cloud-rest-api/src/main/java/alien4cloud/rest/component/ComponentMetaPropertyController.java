package alien4cloud.rest.component;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.rest.common.AbstractMetaPropertyController;
import alien4cloud.rest.internal.model.PropertyRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping({"/rest/components/{id:.+}/properties", "/rest/v1/components/{id:.+}/properties", "/rest/latest/components/{id:.+}/properties"})
@Api(value = "", description = "Operations on Component's meta-properties")
public class ComponentMetaPropertyController extends AbstractMetaPropertyController {

    /**
     * Update or create a property for an component.
     *
     * @param id id of the component
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
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN, Role.COMPONENTS_MANAGER);
        return super.upsertProperty(id, propertyRequest);
    }

    @Override
    protected IMetaProperties getTarget(String id) {
        return alienDAO.findById(NodeType.class, id);
    }
}