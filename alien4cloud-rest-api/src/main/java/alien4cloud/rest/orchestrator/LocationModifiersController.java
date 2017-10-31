package alien4cloud.rest.orchestrator;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manages the location modifiers associated with the location.
 */
@Slf4j
@RestController
@RequestMapping(value = { "/rest/orchestrators/{orchestratorId}/locations/{locationId}/modifiers",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/modifiers",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/modifiers" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Location topology modifiers.", description = "Advanced feature that allows configuration of modifiers associated with this location.", authorizations = {
        @Authorization("ADMIN") })
public class LocationModifiersController {

    /**
     * 
     * @param locationModifierReference
     */
    @ApiOperation(value = "Add a modifier to a location.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> add(@ApiParam(value = "Id of the location", required = true) @PathVariable String locationId,
            LocationModifierReference locationModifierReference) {
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Update the order of a modifier.", authorizations = { @Authorization("ADMIN") })
    public RestResponse<Void> move(@ApiParam(value = "Id of the location", required = true) @PathVariable String locationId, int from, int to) {
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Delete a location modifier at the given index.", authorizations = { @Authorization("ADMIN") })
    public RestResponse<Void> delete(@ApiParam(value = "Id of the location", required = true) @PathVariable String locationId, int index) {
        return RestResponseBuilder.<Void> builder().build();
    }
}