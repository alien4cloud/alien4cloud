package alien4cloud.rest.orchestrator;

import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceService;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.CreateLocationResourceTemplateRequest;
import alien4cloud.rest.orchestrator.model.UpdateLocationResourceTemplatePropertyRequest;
import alien4cloud.rest.orchestrator.model.UpdateLocationResourceTemplateRequest;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Controller that manages resources for orchestrator's locations.
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/orchestrators/{orchestratorId}/locations/{locationId}/resources", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrator's Locations", description = "Manages locations for a given orchestrator.", authorizations = { @Authorization("ADMIN") }, position = 4400)
public class LocationResourcesController {
    @Inject
    private LocationResourceService locationResourceService;

    @ApiOperation(value = "Add resource template to a location.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<LocationResourceTemplate> addResourceTemplate(
            @ApiParam(value = "Id of the orchestrator for which to add resource template.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to add resource template.", required = true) @PathVariable String locationId,
            @RequestBody CreateLocationResourceTemplateRequest resourceTemplateRequest) {
        LocationResourceTemplate createdTemplate = locationResourceService.addResourceTemplate(locationId, resourceTemplateRequest.getResourceName(),
                resourceTemplateRequest.getResourceType());
        return RestResponseBuilder.<LocationResourceTemplate> builder().data(createdTemplate).build();
    }

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
    public RestResponse<Void> updateResourceTemplateProperty(
            @ApiParam(value = "Id of the orchestrator for which to update resource template property.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to update resource template property.", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the location's resource.", required = true) @PathVariable String id,
            @RequestBody UpdateLocationResourceTemplatePropertyRequest updateRequest) {
        locationResourceService.setTemplateProperty(id, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update location's resource's capability template capability property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/template/capabilities/{capabilityName}/properties", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<ConstraintInformation> updateResourceTemplateCapabilityProperty(
            @ApiParam(value = "Id of the orchestrator for which to update resource template capability property.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to update resource template capability property.", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the location's resource.", required = true) @PathVariable String id,
            @ApiParam(value = "Id of the location's resource template capability.", required = true) @PathVariable String capabilityName,
            @RequestBody UpdateLocationResourceTemplatePropertyRequest updateRequest) {
        try {
            locationResourceService.setTemplateCapabilityProperty(id, capabilityName, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
        } catch (ConstraintViolationException e) {
            log.error(
                    "Constraint violation error for property <" + updateRequest.getPropertyName() + "> with value <" + updateRequest.getPropertyValue() + ">",
                    e);
            return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
            log.error("Constraint value violation error for property <" + e.getConstraintInformation().getName() + "> with value <"
                    + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
            return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        }
        return RestResponseBuilder.<ConstraintInformation> builder().data(null).build();
    }


    @ApiOperation(value = "Auto configure the resources, if the location configurator plugin provides a way for.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/auto-configure", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<List<LocationResourceTemplate>> autoConfigureResources(
            @ApiParam(value = "Id of the orchestrator for which to update resource template capability property.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to update resource template capability property.", required = true) @PathVariable String locationId) {
        locationResourceService.deleteGeneratedResources(locationId);
        List<LocationResourceTemplate> generatedResoucres = locationResourceService.autoConfigureResources(locationId);
        return RestResponseBuilder.<List<LocationResourceTemplate>> builder().data(generatedResoucres).build();
    }
}
