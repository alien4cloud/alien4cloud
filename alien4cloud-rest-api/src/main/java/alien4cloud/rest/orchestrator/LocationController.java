package alien4cloud.rest.orchestrator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.validation.Valid;

import org.alien4cloud.secret.services.SecretProviderService;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.google.common.collect.Lists;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.CreateLocationRequest;
import alien4cloud.rest.orchestrator.model.LocationDTO;
import alien4cloud.rest.orchestrator.model.UpdateLocationRequest;
import alien4cloud.rest.secret.model.SecretProviderConfigurationsDTO;
import alien4cloud.ui.form.PojoFormDescriptorGenerator;
import alien4cloud.utils.ReflectionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

/**
 * Controller that manages locations for orchestrators.
 */
@RestController
@RequestMapping(value = { "/rest/orchestrators/{orchestratorId}/locations", "/rest/v1/orchestrators/{orchestratorId}/locations",
        "/rest/latest/orchestrators/{orchestratorId}/locations", "/rest/latest/orchestrators/{orchestratorId}/locations",
        "/rest/latest/orchestrators/{orchestratorId}/locations" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrators Locations", description = "Manages locations for a given orchestrator.", authorizations = {
        @Authorization("ADMIN") }, position = 4400)
public class LocationController {
    @Inject
    private LocationService locationService;
    @Resource(name = "location-resource-service")
    private ILocationResourceService locationResourceService;
    @Resource
    private SecretProviderService secretProviderService;
    @Resource
    private PojoFormDescriptorGenerator pojoFormDescriptorGenerator;

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
        return RestResponseBuilder.<Boolean> builder().data(locationService.delete(orchestratorId, id)).build();
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
        return RestResponseBuilder.<LocationDTO> builder().data(buildLocationDTO(location)).build();
    }

    @ApiOperation(value = "Update the name of an existing location.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> update(@ApiParam(value = "Id of the orchestrator for which the location is defined.") @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location to update", required = true) @PathVariable String id,
            @ApiParam(value = "Location update request, representing the fields to updates and their new values.", required = true) @Valid @NotEmpty @RequestBody UpdateLocationRequest updateRequest) {
        Location location = locationService.getOrFail(id);
        String currentName = location.getName();
        ReflectionUtil.mergeObject(updateRequest, location);
        locationService.ensureNameUnicityAndSave(location, currentName);
        return RestResponseBuilder.<Void> builder().build();
    }

    private SecretProviderConfigurationsDTO getSecretConfigurations(Location location) {
        Set<String> availablePlugins = secretProviderService.getAvailablePlugins();
        Map<String, Map<String, Object>> genericFormDescriptionByPluginName = availablePlugins.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        pluginName -> pojoFormDescriptorGenerator.generateDescriptor(secretProviderService.getPluginConfigurationDescriptor(pluginName)))
                );

        SecretProviderConfigurationsDTO dto = new SecretProviderConfigurationsDTO();
        dto.setCurrentConfiguration(location.getSecretProviderConfiguration());
        dto.setGenericFormByPluginName(genericFormDescriptionByPluginName);

        return dto;
    }

    private LocationDTO buildLocationDTO(Location location) {
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setResources(locationResourceService.getLocationResources(location));
        locationDTO.setLocation(location);
        locationDTO.setSecretProviderConfigurations(getSecretConfigurations(location));
        return locationDTO;
    }
}
