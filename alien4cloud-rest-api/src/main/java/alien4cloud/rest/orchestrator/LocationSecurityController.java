package alien4cloud.rest.orchestrator;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.elasticsearch.common.collect.Lists;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.ResourcePermissionService;
import alien4cloud.security.Subject;
import alien4cloud.security.groups.IAlienGroupDao;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.User;
import alien4cloud.security.users.IAlienUserDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private IAlienGroupDao alienGroupDao;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
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
     * @param locationId The location's id.
     * @param userNames The authorized users.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the location to the users", notes = "Only user with ADMIN role can grant access to another users.")
    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> grantAccessToUsers(@PathVariable String orchestratorId, @PathVariable String locationId, @RequestBody String[] userNames) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.grantPermission(location, Subject.USER, userNames);
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
        resourcePermissionService.revokePermission(location, Subject.USER, username);
        return RestResponseBuilder.<Void> builder().build();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static class UserDTO {
        private String username;
        private String lastName;
        private String firstName;
        private String email;
    }

    /**
     * List all users authorised to access the location.
     *
     * @return list of all users.
     */
    @ApiOperation(value = "List all users authorized to access the location", notes = "Only user with ADMIN role can list authorized users to the location.")
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<UserDTO>> getAuthorizedUsers(@PathVariable String orchestratorId, @PathVariable String locationId) {
        Location location = getLocation(orchestratorId, locationId);
        List<UserDTO> userDTOs = Lists.newArrayList();
        if (location.getUserPermissions() != null && location.getUserPermissions().size() > 0) {
            List<User> users = alienUserDao.find(location.getUserPermissions().keySet().toArray(new String[location.getUserPermissions().size()]));
            users.sort(Comparator.comparing(User::getUsername));
            userDTOs = users.stream().map(user -> new UserDTO(user.getUsername(), user.getLastName(), user.getFirstName(), user.getEmail()))
                    .collect(Collectors.toList());
        }
        return RestResponseBuilder.<List<UserDTO>> builder().data(userDTOs).build();
    }

    /**
     * Grant access to the location to the groups (deploy on the location)
     *
     * @param locationId The location's id.
     * @param groupIds The authorized groups.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the location to the groups", notes = "Only user with ADMIN role can grant access to a group.")
    @RequestMapping(value = "/groups", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> grantAccessToGroups(@PathVariable String orchestratorId, @PathVariable String locationId, @RequestBody String[] groupIds) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.grantPermission(location, Subject.GROUP, groupIds);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Revoke the group's authorisation to access the location
     *
     * @param locationId The id of the location.
     * @param groupId The authorized group.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the group's authorisation to access the location", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/groups/{groupId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> revokeGroupAccess(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String groupId) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.revokePermission(location, Subject.GROUP, groupId);
        return RestResponseBuilder.<Void> builder().build();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static class GroupDTO {
        private String id;
        private String name;
        private String email;
        private String description;
    }

    /**
     * List all groups authorised to access the location.
     *
     * @return list of all authorised groups.
     */
    @ApiOperation(value = "List all groups authorized to access the location", notes = "Only user with ADMIN role can list authorized groups to the location.")
    @RequestMapping(value = "/groups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<GroupDTO>> getAuthorizedGroups(@PathVariable String orchestratorId, @PathVariable String locationId) {
        Location location = getLocation(orchestratorId, locationId);
        List<GroupDTO> groupDTOS = Lists.newArrayList();
        if (location.getGroupPermissions() != null && location.getGroupPermissions().size() > 0) {
            List<Group> groups = alienGroupDao.find(location.getGroupPermissions().keySet().toArray(new String[location.getGroupPermissions().size()]));
            groups.sort(Comparator.comparing(Group::getName));
            groupDTOS = groups.stream().map(group -> new GroupDTO(group.getId(), group.getName(), group.getEmail(), group.getDescription()))
                    .collect(Collectors.toList());
        }
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groupDTOS).build();
    }

    /**
     * Grant access to the location to the application (deploy on the location)
     *
     * @param locationId The location's id.
     * @param applicationName The authorized application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the location to the application", notes = "Only user with ADMIN role can grant access to an application.")
    @RequestMapping(value = "/applications/{applicationName}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> grantAccessToApplication(@PathVariable String orchestratorId, @PathVariable String locationId,
            @PathVariable String applicationName) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.grantPermission(location, Subject.APPLICATION, applicationName);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Revoke the application's authorisation to access the location
     *
     * @param locationId The id of the location.
     * @param applicationName The authorized application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the application's authorisation to access the location", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/applications/{applicationName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> revokeApplicationAccess(@PathVariable String orchestratorId, @PathVariable String locationId,
            @PathVariable String applicationName) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.revokePermission(location, Subject.APPLICATION, applicationName);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * List all applications authorised to access the location.
     *
     * @return list of all application.
     */
    @ApiOperation(value = "List all applications authorized to access the location", notes = "Only user with ADMIN role can list authorized applications to the location.")
    @RequestMapping(value = "/applications", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<Application>> getAuthorizedApplications(@PathVariable String orchestratorId, @PathVariable String locationId) {
        Location location = getLocation(orchestratorId, locationId);
        List<Application> applications;
        if (location.getApplicationPermissions() != null && location.getApplicationPermissions().size() > 0) {
            applications = alienDAO.findByIds(Application.class,
                    location.getApplicationPermissions().keySet().toArray(new String[location.getApplicationPermissions().size()]));
            applications.sort(Comparator.comparing(Application::getName));
        } else {
            applications = Lists.newArrayList();
        }
        return RestResponseBuilder.<List<Application>> builder().data(applications).build();
    }

    /**
     * Grant access to the location to the environment (deploy on the location)
     *
     * @param locationId The location's id.
     * @param environmentId The authorized environment.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the location to the environment", notes = "Only user with ADMIN role can grant access to an environment.")
    @RequestMapping(value = "/environments/{environmentId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> grantAccessToEnvironment(@PathVariable String orchestratorId, @PathVariable String locationId,
            @PathVariable String environmentId) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.grantPermission(location, Subject.ENVIRONMENT, environmentId);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Revoke the environment's authorisation to access the location
     *
     * @param locationId The id of the location.
     * @param environmentId The authorized environment.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the environment's authorisation to access the location", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/environments/{environmentId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> revokeEnvironmentAccess(@PathVariable String orchestratorId, @PathVariable String locationId,
            @PathVariable String environmentId) {
        Location location = getLocation(orchestratorId, locationId);
        resourcePermissionService.revokePermission(location, Subject.ENVIRONMENT, environmentId);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * List all environments authorised to access the location.
     *
     * @return list of all environments.
     */
    @ApiOperation(value = "List all environments authorized to access the location", notes = "Only user with ADMIN role can list authorized environments to the location.")
    @RequestMapping(value = "/environments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<ApplicationEnvironment>> getAuthorizedEnvironments(@PathVariable String orchestratorId, @PathVariable String locationId) {
        Location location = getLocation(orchestratorId, locationId);
        List<ApplicationEnvironment> environments;
        if (location.getEnvironmentPermissions() != null && location.getEnvironmentPermissions().size() > 0) {
            environments = alienDAO.findByIds(ApplicationEnvironment.class,
                    location.getEnvironmentPermissions().keySet().toArray(new String[location.getEnvironmentPermissions().size()]));
            environments.sort(Comparator.comparing(ApplicationEnvironment::getName));
        } else {
            environments = Lists.newArrayList();
        }
        return RestResponseBuilder.<List<ApplicationEnvironment>> builder().data(environments).build();
    }
}
