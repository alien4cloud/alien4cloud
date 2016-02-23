package alien4cloud.rest.orchestrator;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.orchestrators.services.OrchestratorSecurityService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({"/rest/orchestrators/{orchestratorId}/roles/", "/rest/v1/orchestrators/{orchestratorId}/roles/"})
public class OrchestratorRolesController {
    @Resource
    private OrchestratorSecurityService orchestratorSecurityService;

    /**
     * Add a role to a user on all locations of a specific orchestrator
     *
     * @param orchestratorId The orchestrator id.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the orchestrator.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a user on all locations of a specific orchestrator", notes = "Only user with ADMIN role can assign any role to another user.")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> addUserRole(@PathVariable String orchestratorId, @PathVariable String username, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        orchestratorSecurityService.addUserRoleOnAllLocations(orchestratorId, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a role to a group on all locations of a specific orchestrator
     *
     * @param orchestratorId The id of the orchestrator.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the group on the orchestrator.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a group on all locations of a specific orchestrator", notes = "Only user with ADMIN role can assign any role to a group of users.")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> addGroupRole(@PathVariable String orchestratorId, @PathVariable String groupId, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        orchestratorSecurityService.addGroupRoleOnAllLocations(orchestratorId, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on all locations of a specific orchestrator
     *
     * @param orchestratorId The id of the orchestrator.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the orchestrator.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role to a user on all locations of a specific orchestrator", notes = "Only user with ADMIN role can unassign any role to another user.")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> removeUserRole(@PathVariable String orchestratorId, @PathVariable String username, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        orchestratorSecurityService.removeUserRoleOnAllLocations(orchestratorId, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on all locations of a specific orchestrator
     *
     * @param orchestratorId The id of the orchestrator.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the user on the orchestrator.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role of a group on all locations of a specific orchestrator", notes = "Only user with ADMIN role can unassign any role to a group.")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> removeGroupRole(@PathVariable String orchestratorId, @PathVariable String groupId, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        orchestratorSecurityService.removeGroupRoleOnAllLocations(orchestratorId, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }
}
