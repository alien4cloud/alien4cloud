package alien4cloud.rest.application;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import alien4cloud.security.ResourceRoleService;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({ "/rest/applications/{applicationId:.+}/environments/{applicationEnvironmentId:.+}/roles",
        "/rest/v1/applications/{applicationId:.+}/environments/{applicationEnvironmentId:.+}/roles",
        "/rest/latest/applications/{applicationId:.+}/environments/{applicationEnvironmentId:.+}/roles" })
@Api(value = "", description = "Manages application's environments")
public class ApplicationEnvironmentRolesController {
    @Resource
    private ResourceRoleService resourceRoleService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private ApplicationService applicationService;

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
        // Check if user has at least one role on the application or the environments
        Set<String> applicationRoles = application.getUserRoles() != null ? application.getUserRoles().get(username) : new HashSet<>();
        List<Set<String>> environmentRoles = Arrays.stream(applicationEnvironmentService.getByApplicationId(applicationId))
                .map(applicationEnvironment -> applicationEnvironment.getUserRoles() != null ? applicationEnvironment.getUserRoles().get(username) : null)
                .filter(roles -> roles != null).collect(Collectors.toList());
        if (mustRemoveApplicationUserRole(applicationRoles, environmentRoles)) {
            // If we are here, it means that we must take out the APPLICATION_USER role for application as user does not have any other role than that
            resourceRoleService.removeUserRole(application, username, ApplicationRole.APPLICATION_USER.toString());
        }
    }

    private boolean mustRemoveApplicationUserRole(Set<String> applicationRoles, List<Set<String>> allEnvironmentRoles) {
        if (applicationRoles == null || applicationRoles.isEmpty()) {
            // User has no role on the application, nothing to do
            return false;
        }
        // Not take into account application user role it-self
        int appUserCount = applicationRoles.contains(ApplicationRole.APPLICATION_USER.toString()) ? 1 : 0;
        if (applicationRoles.size() > appUserCount) {
            // Has other role than APPLICATION_USER, then APPLICATION_USER role is necessary
            return false;
        }
        for (Set<String> environmentRoles : allEnvironmentRoles) {
            if (environmentRoles != null && !environmentRoles.isEmpty()) {
                // An environment role imply an application user role
                return false;
            }
        }
        return true;
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
        // Check if group has at least one role on the application or the environments
        Set<String> applicationRoles = application.getGroupRoles() != null ? application.getGroupRoles().get(groupId) : new HashSet<>();
        List<Set<String>> environmentRoles = Arrays.stream(applicationEnvironmentService.getByApplicationId(applicationId))
                .map(applicationEnvironment -> applicationEnvironment.getGroupRoles() != null ? applicationEnvironment.getGroupRoles().get(groupId) : null)
                .filter(roles -> roles != null).collect(Collectors.toList());
        if (mustRemoveApplicationUserRole(applicationRoles, environmentRoles)) {
            // If we are here, it means that we must take out the APPLICATION_USER role for application as group does not have any other role than that
            resourceRoleService.removeGroupRole(application, groupId, ApplicationRole.APPLICATION_USER.toString());
        }
    }
}
