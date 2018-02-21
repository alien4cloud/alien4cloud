package alien4cloud.rest.orchestrator.policies;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplateWithDependencies;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.AbstractLocationResourcesController;
import alien4cloud.rest.orchestrator.model.CreateLocationResourceTemplateRequest;
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
@RequestMapping(value = { "/rest/orchestrators/{orchestratorId}/locations/{locationId}/policies",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/policies",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/policies" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrator Location Policies Resources", description = "Manages locations policies for a given orchestrator.", authorizations = {
        @Authorization("ADMIN") }, position = 4400)
public class LocationPolicyResourcesController extends AbstractLocationResourcesController {

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

    @ApiOperation(value = "Duplicate a  policy template resource on a location .", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/duplicate", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<LocationResourceTemplateWithDependencies> duplicateResourceTemplate(
            @ApiParam(value = "Id of the orchestrator for which to duplicate policy template.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location of the orchestrator to duplicate policy template.", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the policy template to duplicate.", required = true) @PathVariable String id) {
        LocationResourceTemplateWithDependencies duplicatedTemplate = locationResourceService.duplicatePolicyTemplate(id);
        return RestResponseBuilder.<LocationResourceTemplateWithDependencies> builder().data(duplicatedTemplate).build();
    }

}
