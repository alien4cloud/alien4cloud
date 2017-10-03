package alien4cloud.rest.orchestrator;

import javax.inject.Inject;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplateWithDependencies;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.CreateLocationResourceTemplateRequest;
import alien4cloud.rest.orchestrator.model.UpdateLocationResourceTemplatePropertyRequest;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.utils.RestConstraintValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller that manages policies resources for orchestrator's locations.
 */
@Slf4j
@RestController
@RequestMapping(value = { "/rest/orchestrators/{orchestratorId}/locations/{locationId}/resources/policies",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/resources/policies",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/resources/policies" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrator Location Policies Resources", description = "Manages locations policies for a given orchestrator.", authorizations = {
        @Authorization("ADMIN") }, position = 4400)
public class LocationPolicyResourcesController {
    @Inject
    @Lazy(true)
    private ILocationResourceService locationResourceService;

    @ApiOperation(value = "Add policy template to a location.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<LocationResourceTemplateWithDependencies> addPolicyLocationResourceTemplate(
            @ApiParam(value = "Id of the orchestrator for which to add policy template.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to add policy template.", required = true) @PathVariable String locationId,
            @RequestBody CreateLocationResourceTemplateRequest resourceTemplateRequest) {

        LocationResourceTemplateWithDependencies createdTemplate = locationResourceService.addPolicyTemplateFromArchive(locationId,
                resourceTemplateRequest.getResourceName(), resourceTemplateRequest.getResourceType(), resourceTemplateRequest.getArchiveName(),
                resourceTemplateRequest.getArchiveVersion());
        return RestResponseBuilder.<LocationResourceTemplateWithDependencies> builder().data(createdTemplate).build();
    }

    @ApiOperation(value = "Delete location's policy resource.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> deletePolicyResourceTemplate(
            @ApiParam(value = "Id of the orchestrator for which to delete policy resource template.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to delete policy resource template.", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the location's policy resource.", required = true) @PathVariable String id) {
        locationResourceService.deleteResourceTemplate(PolicyLocationResourceTemplate.class, id);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update location's policy resource template property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/properties", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<ConstraintUtil.ConstraintInformation> updatePolicyResourceTemplateProperty(
            @ApiParam(value = "Id of the orchestrator for which to update the policy resource template property.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to update the policy resource template property.", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the location's policy resource.", required = true) @PathVariable String id,
            @RequestBody UpdateLocationResourceTemplatePropertyRequest updateRequest) {
        try {
            locationResourceService.setTemplateProperty(PolicyLocationResourceTemplate.class, id, updateRequest.getPropertyName(),
                    updateRequest.getPropertyValue());
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().build();
        } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
            return RestConstraintValidator.fromException(e, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
        }
    }

}
