package alien4cloud.orchestrators.rest;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.rest.model.CreateLocationRequest;
import alien4cloud.orchestrators.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.util.List;

/**
 * Controller that manages locations for orchestrators.
 */
@RestController
@RequestMapping(value = "/rest/orchestrators/{orchestratorId}/locations", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrator's Locations", description = "Manages locations for a given orchestrator.", authorizations = { @Authorization("ADMIN") }, position = 4400)
public class LocationController {
    @Inject
    private LocationService locationService;
    @Inject
    private OrchestratorService orchestratorService;

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new location.", authorizations = { @Authorization("ADMIN") })
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String> create(@ApiParam(value = "Id of the orchestrator for which the location is defined.") @PathVariable String orchestratorId,
            @ApiParam(value = "Request for location creation", required = true) @Valid @RequestBody CreateLocationRequest locationRequest) {
        String id = locationService.create(orchestratorId, locationRequest.getName(), locationRequest.getInfrastructureType());
        return RestResponseBuilder.<String> builder().data(id).build();
    }

    // @ApiOperation(value = "Update the name of an existing orchestrators.", authorizations = { @Authorization("ADMIN") })
    // @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    // @PreAuthorize("hasAuthority('ADMIN')")
    // @Audit
    // public void update(@ApiParam(value = "Id of the orchestrators to update.", required = true) @PathVariable @Valid @NotEmpty String id,
    // @ApiParam(value = "Orchestrator's new name.", required = true) @Valid @NotEmpty @RequestBody String name) {
    // // TODO Update a location
    // }

    @ApiOperation(value = "Delete an existing location.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public void delete(@ApiParam(value = "Id of the orchestrator for which to get all locations.", required = true) @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the orchestrators to delete.", required = true) @PathVariable String id) {
        locationService.delete(id);
    }

    @ApiOperation(value = "Get all locations for a given orchestrator.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<Location>> getAll(@ApiParam(value = "Id of the orchestrator for which to get all locations.") @PathVariable String orchestratorId) {
        List<Location> result = locationService.getAll(orchestratorId);
        return RestResponseBuilder.<List<Location>> builder().data(result).build();
    }

    @ApiOperation(value = "Get a location from it's id.")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Location> get(@ApiParam(value = "Id of the orchestrator for which the location is defined.") @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location to get", required = true) @PathVariable String id) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        Location location = locationService.getOrFail(id);
        AuthorizationUtil.checkAuthorizationForCloud(location, DeployerRole.DEPLOYER);
        return RestResponseBuilder.<Location> builder().data(location).build();
    }
}
