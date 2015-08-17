package alien4cloud.rest.cloud;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.cloud.CloudService;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.ResourceRoleService;
import alien4cloud.security.model.Role;

import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/clouds/{cloudId}/roles/")
public class CloudRolesController {
    @Resource
    private CloudService cloudService;
    @Resource
    private ResourceRoleService resourceRoleService;


    /**
     * Add a role to a user on a specific cloud
     *
     * @param cloudId The cloud id.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the cloud.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a user on a specific cloud", notes = "Only user with ADMIN role can assign any role to another user.")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> addUserRole(@PathVariable String cloudId, @PathVariable String username, @PathVariable String role) {

        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        resourceRoleService.addUserRole(cloud, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a role to a group on a specific cloud
     *
     * @param cloudId The id of the cloud.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the group on the cloud.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a group on a specific cloud", notes = "Only user with ADMIN role can assign any role to a group of users.")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> addGroupRole(@PathVariable String cloudId, @PathVariable String groupId, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        resourceRoleService.addGroupRole(cloud, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific cloud
     *
     * @param cloudId The id of the cloud.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the cloud.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role to a user on a specific cloud", notes = "Only user with ADMIN role can unassign any role to another user.")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> removeUserRole(@PathVariable String cloudId, @PathVariable String username, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        resourceRoleService.removeUserRole(cloud, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific cloud
     *
     * @param cloudId The id of the cloud.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the user on the cloud.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role of a group on a specific cloud", notes = "Only user with ADMIN role can unassign any role to a group.")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> removeGroupRole(@PathVariable String cloudId, @PathVariable String groupId, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        resourceRoleService.removeGroupRole(cloud, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }
}
