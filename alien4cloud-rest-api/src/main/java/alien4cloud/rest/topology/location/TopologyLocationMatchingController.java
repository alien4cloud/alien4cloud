package alien4cloud.rest.topology.location;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.model.orchestrators.locations.LocationMatch;
import alien4cloud.orchestrators.services.LocationMatchingService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Controller that manages topology and locations matching
 *
 */

@RestController
@RequestMapping(value = "/rest/topology/{topologyId}/locations", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrator Configuration", description = "Get and update orchestrator configuration.", authorizations = { @Authorization("ADMIN") }, position = 4310)
public class TopologyLocationMatchingController {

    @Resource
    private LocationMatchingService locationMatchingService;

    @ApiOperation(value = "Update the configuration for an orchestrator.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    // @Audit
    public RestResponse<List<LocationMatch>> match(@PathVariable String topologyId) {

        // TODO check deployer authorizations
        RestResponseBuilder<List<LocationMatch>> responseBuilder = RestResponseBuilder.builder();

        List<LocationMatch> matchedLocation = locationMatchingService.match(topologyId);

        return responseBuilder.data(matchedLocation).build();

    }
}
