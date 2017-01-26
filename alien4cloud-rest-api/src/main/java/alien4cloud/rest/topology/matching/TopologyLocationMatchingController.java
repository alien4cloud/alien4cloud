package alien4cloud.rest.topology.matching;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.deployment.matching.services.location.LocationMatchingService;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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

    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    @Resource
    private ApplicationService applicationService;

    @ApiOperation(value = "Retrieve the list of locations on which the current user can deploy the topology.")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<ILocationMatch>> match(@PathVariable String topologyId, @RequestParam(required = false) String environmentId) {
        List<ILocationMatch> matchedLocation;
        if (StringUtils.isNotBlank(environmentId)) {
            ApplicationEnvironment environment = applicationEnvironmentService.getOrFail(environmentId);
            Application application = applicationService.getOrFail(environment.getApplicationId());
            AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
            matchedLocation = locationMatchingService.match(topologyId, environment);
        } else {
            matchedLocation = locationMatchingService.match(topologyId, null);
        }
        return RestResponseBuilder.<List<ILocationMatch>> builder().data(matchedLocation).build();
    }
}
