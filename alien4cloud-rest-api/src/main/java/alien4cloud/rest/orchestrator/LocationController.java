package alien4cloud.rest.orchestrator;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.CreateLocationRequest;
import alien4cloud.rest.orchestrator.model.LocationDTO;
import alien4cloud.rest.orchestrator.model.UpdateLocationRequest;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

/**
 * Controller that manages locations for orchestrators.
 */
@RestController
@RequestMapping(value = {"/rest/orchestrators/{orchestratorId}/locations", "/rest/v1/orchestrators/{orchestratorId}/locations"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrators Locations", description = "Manages locations for a given orchestrator.", authorizations = { @Authorization("ADMIN") }, position = 4400)
public class LocationController {
    @Inject
    private LocationService locationService;
    @Inject
    private LocationResourceService locationResourceService;

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

    @ApiOperation(value = "Delete an existing location.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Boolean> delete(@ApiParam(value = "Id of the orchestrator for which the location is defined.") @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location to delete.", required = true) @PathVariable String id) {
        return RestResponseBuilder.<Boolean> builder().data(locationService.delete(id)).build();
    }

    @ApiOperation(value = "Get all locations for a given orchestrator.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<LocationDTO>> getAll(
            @ApiParam(value = "Id of the orchestrator for which to get all locations.") @PathVariable String orchestratorId) {
        List<Location> locations = locationService.getAll(orchestratorId);
        List<LocationDTO> locationDTOs = Lists.newArrayList();
        for (Location location : locations) {
            locationDTOs.add(buildLocationDTO(location));
        }
        return RestResponseBuilder.<List<LocationDTO>> builder().data(locationDTOs).build();
    }

    @ApiOperation(value = "Get a location from it's id.")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<LocationDTO> get(@ApiParam(value = "Id of the orchestrator for which the location is defined.") @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location to get", required = true) @PathVariable String id) {
        Location location = locationService.getOrFail(id);
        AuthorizationUtil.checkAuthorizationForLocation(location, DeployerRole.DEPLOYER);
        return RestResponseBuilder.<LocationDTO> builder().data(buildLocationDTO(location)).build();
    }

    @ApiOperation(value = "Update the name of an existing location.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> update(
            @ApiParam(value = "Id of the orchestrator for which the location is defined.") @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location to update", required = true) @PathVariable String id,
            @ApiParam(value = "Location update request, representing the fields to updates and their new values.", required = true) @Valid @NotEmpty @RequestBody UpdateLocationRequest updateRequest) {
        Location location = locationService.getOrFail(id);
        String currentName = location.getName();
        ReflectionUtil.mergeObject(updateRequest, location);
        locationService.ensureNameUnicityAndSave(location, currentName);
        return RestResponseBuilder.<Void> builder().build();
    }

    private LocationDTO buildLocationDTO(Location location) {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setResources(locationResourceService.getLocationResources(location));
        locationDTO.setLocation(location);
        return locationDTO;
    }
}
