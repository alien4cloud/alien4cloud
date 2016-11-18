package alien4cloud.rest.orchestrator;

import javax.annotation.Resource;

import alien4cloud.model.orchestrators.Orchestrator;
import io.swagger.annotations.Api;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.ResourceRoleService;
import alien4cloud.security.model.Role;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({"/rest/orchestrators/{orchestratorId}/locations/{locationId}/roles/", "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/roles/", "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/roles/"})
@Api(value = "", description = "Operations on orchestrator location roles")
public class LocationRolesController {
    @Resource
    private LocationService locationsService;
    @Resource
    private ResourceRoleService resourceRoleService;
    @Resource
    private OrchestratorService orchestratorService;

    /**
     * Add a role to a user on a specific location
     *
     * @param locationId The locations id.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the location.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a user on a specific location", notes = "Only user with ADMIN role can assign any role to another user.")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> addUserRole(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String username, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Location location = locationsService.getOrFail(locationId);
        resourceRoleService.addUserRole(location, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a role to a group on a specific location
     *
     * @param locationId The id of the locations.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the group on the location.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a group on a specific location", notes = "Only user with ADMIN role can assign any role to a group of users.")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> addGroupRole(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String groupId, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Location location = locationsService.getOrFail(locationId);
        resourceRoleService.addGroupRole(location, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific location
     *
     * @param locationId The id of the location.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the locations.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role to a user on a specific location", notes = "Only user with ADMIN role can unassign any role to another user.")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> removeUserRole(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String username, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        if (!orchestrator.getAuthorizedUsers().contains(username)) {
            Location location = locationsService.getOrFail(locationId);
            resourceRoleService.removeUserRole(location, username, role);
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific location
     *
     * @param locationId The id of the locations.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the user on the location.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role of a group on a specific location", notes = "Only user with ADMIN role can unassign any role to a group.")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> removeGroupRole(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String groupId, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        if (!orchestrator.getAuthorizedGroups().contains(groupId)) {
            Location location = locationsService.getOrFail(locationId);
            resourceRoleService.removeGroupRole(location, groupId, role);
        }
        return RestResponseBuilder.<Void> builder().build();
    }
}
