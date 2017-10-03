package alien4cloud.rest.orchestrator;

import javax.inject.Inject;

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
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
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

}
