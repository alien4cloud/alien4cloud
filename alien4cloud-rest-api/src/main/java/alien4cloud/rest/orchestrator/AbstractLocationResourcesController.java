package alien4cloud.rest.orchestrator;

import javax.inject.Inject;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.UpdateLocationResourceTemplatePropertyRequest;
import alien4cloud.rest.orchestrator.model.UpdateLocationResourceTemplateRequest;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.utils.RestConstraintValidator;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

public abstract class AbstractLocationResourcesController {

    @Inject
    @Lazy(true)
    protected ILocationResourceService locationResourceService;

    @ApiOperation(value = "Delete location's resource.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> deleteResourceTemplate(
            @ApiParam(value = "Id of the orchestrator for which to delete resource template.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to delete resource template.", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the location's resource.", required = true) @PathVariable String id) {
        locationResourceService.deleteResourceTemplate(id);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update location's resource.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> updateResourceTemplate(
            @ApiParam(value = "Id of the orchestrator for which to update resource template.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to update resource template.", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the location's resource.", required = true) @PathVariable String id,
            @RequestBody UpdateLocationResourceTemplateRequest mergeRequest) {
        locationResourceService.merge(mergeRequest, id);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update location's resource's template property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/template/properties", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<ConstraintUtil.ConstraintInformation> updateResourceTemplateProperty(
            @ApiParam(value = "Id of the orchestrator for which to update resource template property.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to update resource template property.", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the location's resource.", required = true) @PathVariable String id,
            @RequestBody UpdateLocationResourceTemplatePropertyRequest updateRequest) {
        try {
            locationResourceService.setTemplateProperty(id, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().build();
        } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
            return RestConstraintValidator.fromException(e, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
        }
    }
}
