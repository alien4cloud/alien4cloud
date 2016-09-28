package alien4cloud.rest.application;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.model.application.Application;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.ResourceRoleService;
import alien4cloud.security.model.ApplicationRole;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Service that allows managing applications roles.
 */
@RestController
@RequestMapping({"/rest/applications/{applicationId:.+}/roles", "/rest/v1/applications/{applicationId:.+}/roles", "/rest/latest/applications/{applicationId:.+}/roles"})
@Api(value = "", description = "Operations on applications roles")
public class ApplicationRolesController {
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ResourceRoleService resourceRoleService;

    /**
     * Add a role to a user on a specific application
     *
     * @param applicationId The id of the application.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a user on a specific application", notes = "Any user with application role APPLICATION_MANAGER can assign any role to another user. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> addUserRole(@PathVariable String applicationId, @PathVariable String username, @PathVariable String role) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.addUserRole(application, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a role to a group on a specific application
     *
     * @param applicationId The id of the application.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the group on the application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a group on a specific application", notes = "Any user with application role APPLICATION_MANAGER can assign any role to a group of users. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> addGroupRole(@PathVariable String applicationId, @PathVariable String groupId, @PathVariable String role) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.addGroupRole(application, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific application
     *
     * @param applicationId The id of the application.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role to a user on a specific application", notes = "Any user with application role APPLICATION_MANAGER can unassign any role to another user. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/users/{username}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> removeUserRole(@PathVariable String applicationId, @PathVariable String username, @PathVariable String role) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.removeUserRole(application, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific application
     *
     * @param applicationId The id of the application.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the user on the application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role of a group on a specific application", notes = "Any user with application role APPLICATION_MANAGER can un-assign any role to a group. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/groups/{groupId}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> removeGroupRole(@PathVariable String applicationId, @PathVariable String groupId, @PathVariable String role) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        resourceRoleService.removeGroupRole(application, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }
}
