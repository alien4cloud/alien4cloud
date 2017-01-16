package alien4cloud.rest.orchestrator;

import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.ResourcePermissionService;
import alien4cloud.security.Subject;
import alien4cloud.security.model.User;
import alien4cloud.security.users.IAlienUserDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({ "/rest/orchestrators/{orchestratorId}/locations/{locationId}/security/",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/security/",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/security/" })
@Api(value = "", description = "Orchestrator security operations")
public class LocationSecurityController {
    @Resource
    private LocationService locationService;
    @Resource
    private IAlienUserDao alienUserDao;
    @Resource
    private ResourcePermissionService resourcePermissionService;

    private Location getLocation(String orchestratorId, String locationId) {
        Location location = locationService.getOrFail(locationId);
        if (!Objects.equals(location.getOrchestratorId(), orchestratorId)) {
            throw new NotFoundException("Orchestrator id " + orchestratorId + " does not exist or does not have the location " + locationId);
        }
        return location;
    }

    /**
     * Grant access to the location to the user (deploy on the location)
     *
     * @param locationId The locations id.
     * @param username The authorized user.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the location to the user", notes = "Only user with ADMIN role can grant access to another user.")
    @RequestMapping(value = "/users/{username}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> grantAccessToUser(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String username) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.grantAdminPermission(location, Subject.USER, username);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Revoke the user's authorisation to access the location
     *
     * @param locationId The id of the location.
     * @param username The authorized user.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the user's authorisation to access the location", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> revokeUserAccess(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String username) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.revokeAdminPermission(location, Subject.USER, username);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * List all users authorised to access the location.
     * 
     * @return list of all users.
     */
    @ApiOperation(value = "List all users authorized to access the location", notes = "Only user with ADMIN role can list authorized users to the location.")
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<User>> getAuthorizedUsers(@PathVariable String orchestratorId, @PathVariable String locationId) {
        Location location = getLocation(orchestratorId, locationId);
        List<User> users = alienUserDao.find(location.getUserPermissions().keySet().toArray(new String[location.getUserPermissions().size()]));
        return RestResponseBuilder.<List<User>> builder().data(users).build();
    }
}
