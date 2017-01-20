package alien4cloud.rest.topology.matching;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.deployment.matching.services.location.LocationMatchingService;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

/**
 * Controller that manages topology and locations matching
 *
 */

@RestController
@RequestMapping(value = { "/rest/topologies/{topologyId}/locations", "/rest/v1/topologies/{topologyId}/locations",
        "/rest/latest/topologies/{topologyId}/locations" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Location matching", description = "Get matching options for a given topology.", position = 4310)
public class TopologyLocationMatchingController {

    @Resource
    private LocationMatchingService locationMatchingService;

    @ApiOperation(value = "Retrieve the list of locations on which the current user can deploy the topology.")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<ILocationMatch>> match(@PathVariable String topologyId, @RequestParam(required = false) String environmentId) {
        // TODO check deployer authorizations
        RestResponseBuilder<List<ILocationMatch>> responseBuilder = RestResponseBuilder.builder();

        List<ILocationMatch> matchedLocation = locationMatchingService.match(topologyId, environmentId);

        return responseBuilder.data(matchedLocation).build();
    }
}
