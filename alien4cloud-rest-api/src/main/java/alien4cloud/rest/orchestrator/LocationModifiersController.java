package alien4cloud.rest.orchestrator;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import alien4cloud.orchestrators.locations.services.LocationModifierService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Manages the location modifiers associated with the location.
 */
@RestController
@RequestMapping(value = { "/rest/orchestrators/{orchestratorId}/locations/{locationId}/modifiers",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/modifiers",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/modifiers" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Location topology modifiers.", description = "Advanced feature that allows configuration of modifiers associated with this location.", authorizations = {
        @Authorization("ADMIN") })
public class LocationModifiersController {
    @Resource
    private LocationService locationService;
    @Resource
    private LocationModifierService locationModifierService;

    @ApiOperation(value = "Add a modifier to a location.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> add(@ApiParam(value = "Id of the location", required = true) @PathVariable String locationId,
            @ApiParam(value = "The location modifier to add", required = true) @RequestBody LocationModifierReference locationModifierReference) {
        Location location = locationService.getOrFail(locationId);
        locationModifierService.add(location, locationModifierReference);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update the order of a modifier.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/from/{from}/to/{to}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> move(@ApiParam(value = "Id of the location", required = true) @PathVariable String locationId,
            @ApiParam(value = "From index", required = true) @PathVariable int from, @ApiParam(value = "To index", required = true) @PathVariable int to) {
        Location location = locationService.getOrFail(locationId);
        locationModifierService.move(location, from, to);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Delete a location modifier at the given index.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{index}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> delete(@ApiParam(value = "Id of the location", required = true) @PathVariable String locationId,
            @ApiParam(value = "Index of the location modifier to delete", required = true) @PathVariable int index) {
        Location location = locationService.getOrFail(locationId);
        locationModifierService.remove(location, index);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Get all modifiers for a given location.")
    @RequestMapping(method = RequestMethod.GET)
    public RestResponse<List<LocationModifierReference>> getAllModifiers(
            @ApiParam(value = "Id of the location for which to get all modifiers.") @PathVariable String locationId) {
        Location location = locationService.getOrFail(locationId);
        List<LocationModifierReference> modifiers = location.getModifiers();
        if (modifiers == null) {
            modifiers = Lists.newArrayList();
        }
        return RestResponseBuilder.<List<LocationModifierReference>> builder().data(modifiers).build();
    }
}