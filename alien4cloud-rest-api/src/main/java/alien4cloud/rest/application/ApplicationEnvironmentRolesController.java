package alien4cloud.rest.application;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.ResourceRoleService;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.users.UserService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/applications/{applicationId:.+}/environments/{applicationEnvironmentId:.+}/roles")
@Api(value = "", description = "Manages application's environments")
public class ApplicationEnvironmentRolesController {
    @Resource
    private ResourceRoleService resourceRoleService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private UserService userService;

    /**
     * Add a role to a user on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param username user for who to add role
     * @param role the application role to add to this user
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a user on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can assign any role to another user. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> addUserRole(@PathVariable String applicationEnvironmentId, @PathVariable String username, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.addUserRole(applicationEnvironment, username, role);
        handleAddUserRoleOnApplication(applicationEnvironment.getApplicationId(), username);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a role to a group on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param groupId The id of the group to update roles
     * @param role The role to add to the group on the application environment from {@link ApplicationEnvironmentRole}
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a group on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can assign any role to a group of users. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> addGroupRole(@PathVariable String applicationEnvironmentId, @PathVariable String groupId, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.addGroupRole(applicationEnvironment, groupId, role);
        handleAddGrpRoleOnApplication(applicationEnvironment.getApplicationId(), groupId);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param username The username of the user to update roles
     * @param role The role to add to the user on the application environment
     * @return A {@link Void} {@link RestResponse}
     */
    @ApiOperation(value = "Remove a role to a user on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can unassign any role to another user. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> removeUserRole(@PathVariable String applicationEnvironmentId, @PathVariable String username, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.removeUserRole(applicationEnvironment, username, role);
        handleRemoveUserRoleOnApplication(applicationEnvironment.getApplicationId(), username);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a group on a specific application environment
     *
     * @param applicationEnvironmentId application environment id
     * @param groupId The id of the group to update roles
     * @param role The role to add to the user on the application environment
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role of a group on a specific application environment", notes = "Any user with application role APPLICATION_MANAGER can un-assign any role to a group. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> removeGroupRole(@PathVariable String applicationEnvironmentId, @PathVariable String groupId, @PathVariable String role) {
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.checkAndGetApplicationEnvironment(applicationEnvironmentId,
                ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.removeGroupRole(applicationEnvironment, groupId, role);
        handleRemoveGrpRoleOnApplication(applicationEnvironment.getApplicationId(), groupId);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Handle user roles on the targeted application
     * Any role on an environment implies APPLICATION_USER role on the linked application
     *
     * @param applicationId
     * @param username
     */
    private void handleAddUserRoleOnApplication(String applicationId, String username) {
        Application application = applicationService.getOrFail(applicationId);
        resourceRoleService.addUserRole(application, username, ApplicationRole.APPLICATION_USER.toString());
    }

    /**
     * Handle remove roles on the targeted application
     *
     * @param applicationId
     * @param username
     */
    private void handleRemoveUserRoleOnApplication(String applicationId, String username) {
        Application application = applicationService.getOrFail(applicationId);
        boolean isApplicationUserOnly = AuthorizationUtil.hasUniqueUserRoleOnResource(userService.retrieveUser(username), application,
                ApplicationRole.APPLICATION_USER);
        // check this condition > remove the role only if it is not the only role
        if (!isApplicationUserOnly) {
            resourceRoleService.removeUserRole(application, username, ApplicationRole.APPLICATION_USER.toString());
        }
    }

    /**
     * Handle group roles on the targeted application
     * Any role on an environment implies APPLICATION_USER role on the linked application
     *
     * @param applicationId
     * @param groupId
     */
    private void handleAddGrpRoleOnApplication(String applicationId, String groupId) {
        Application application = applicationService.getOrFail(applicationId);
        resourceRoleService.addGroupRole(application, groupId, ApplicationRole.APPLICATION_USER.toString());
    }

    /**
     * Handle group roles on the targeted application
     *
     * @param applicationId
     * @param groupId
     */
    private void handleRemoveGrpRoleOnApplication(String applicationId, String groupId) {
        Application application = applicationService.getOrFail(applicationId);
        boolean isApplicationGroupOnly = AuthorizationUtil.hasUniqueGroupRoleOnResource(groupId, application, ApplicationRole.APPLICATION_USER);
        if (!isApplicationGroupOnly) {
            resourceRoleService.removeGroupRole(application, groupId, ApplicationRole.APPLICATION_USER.toString());
        }
    }
}
