package alien4cloud.rest.orchestrator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.elasticsearch.common.collect.Lists;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationDTO;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationUpdateRequest;
import alien4cloud.rest.orchestrator.model.GroupDTO;
import alien4cloud.rest.orchestrator.model.UserDTO;
import alien4cloud.security.ResourcePermissionService;
import alien4cloud.security.Subject;
import alien4cloud.security.groups.IAlienGroupDao;
import alien4cloud.security.model.Group;
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
    private IAlienGroupDao alienGroupDao;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;


    /*******************************************************************************************************************************
     *
     * SECURITY ON USERS
     *
     *******************************************************************************************************************************/

    /**
     * Convert a List<User> to List<UserDTO>
     *
     * @param users
     * @return List<UserDTO>
     */
    public static List<UserDTO> convertListUserToListUserDTO(List<User> users) {
        return users.stream().map(user -> new UserDTO(user.getUsername(), user.getLastName(), user.getFirstName(), user.getEmail()))
                .collect(Collectors.toList());
    }

    /**
     * Grant access to the location to the user (deploy on the location)
     *
     * @param locationId The location's id.
     * @param userNames The authorized users.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the location to the users, send back the new authorised users list", notes = "Only user with ADMIN role can grant access to another users.")
    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<UserDTO>> grantAccessToUsers(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestBody String[] userNames) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        resourcePermissionService.grantPermission(location, Subject.USER, userNames);
        List<UserDTO> users = LocationSecurityController.convertListUserToListUserDTO(resourcePermissionService.getAuthorizedUsers(location));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }

    /**
     * Revoke the user's authorisation to access the location
     *
     * @param locationId The id of the location.
     * @param username The authorized user.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the user's authorisation to access the location, send back the new authorised users list", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<UserDTO>> revokeUserAccess(@PathVariable String orchestratorId, @PathVariable String locationId,
            @PathVariable String username) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        resourcePermissionService.revokePermission(location, Subject.USER, username);
        List<UserDTO> users = LocationSecurityController.convertListUserToListUserDTO(resourcePermissionService.getAuthorizedUsers(location));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
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
        Location location = locationService.getLocation(orchestratorId, locationId);
        List<UserDTO> users = LocationSecurityController.convertListUserToListUserDTO(resourcePermissionService.getAuthorizedUsers(location));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }


    /*******************************************************************************************************************************
     *
     * SECURITY ON GROUPS
     *
     *******************************************************************************************************************************/

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
    public synchronized RestResponse<List<GroupDTO>> grantAccessToGroups(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestBody String[] groupIds) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        resourcePermissionService.grantPermission(location, Subject.GROUP, groupIds);
        List<GroupDTO> groups = LocationSecurityController.convertListGroupToListGroupDTO(resourcePermissionService.getAuthorizedGroups(location));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
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
    public synchronized RestResponse<List<GroupDTO>> revokeGroupAccess(@PathVariable String orchestratorId, @PathVariable String locationId,
            @PathVariable String groupId) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        resourcePermissionService.revokePermission(location, Subject.GROUP, groupId);
        List<GroupDTO> groups = LocationSecurityController.convertListGroupToListGroupDTO(resourcePermissionService.getAuthorizedGroups(location));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }

    /**
     * Convert a List<Group> to List<GroupDTO>
     *
     * @param groups
     * @return List<UserDTO>
     */
    public static List<GroupDTO> convertListGroupToListGroupDTO(List<Group> groups) {
        return groups.stream().map(group -> new GroupDTO(group.getId(), group.getName(), group.getEmail(), group.getDescription()))
                .collect(Collectors.toList());
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
        Location location = locationService.getLocation(orchestratorId, locationId);
        List<GroupDTO> groups = LocationSecurityController.convertListGroupToListGroupDTO(resourcePermissionService.getAuthorizedGroups(location));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }

    /**
     * Revoke the application's authorisation to access the location (including all related environments).
     *
     * @param locationId The id of the location.
     * @param applicationId The authorized application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the application's authorisation to access the location", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/applications/{applicationId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<Void> revokeApplicationAccess(@PathVariable String orchestratorId, @PathVariable String locationId,
            @PathVariable String applicationId) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        resourcePermissionService.revokePermission(location, Subject.APPLICATION, applicationId);
        // remove all environments related to this application
        ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationId);
        String[] envIds = new String[aes.length];
        for (int i = 0; i < envIds.length; i++) {
            envIds[i] = aes[i].getId();
        }
        resourcePermissionService.revokePermission(location, Subject.ENVIRONMENT, envIds);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Update applications/environments authorized to access the location.
     */
    @ApiOperation(value = "Update applications/environments authorized to access the location", notes = "Only user with ADMIN role can update authorized applications/environments for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized RestResponse<Void> updateAuthorizedEnvironmentsPerApplication(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestBody ApplicationEnvironmentAuthorizationUpdateRequest request) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        if (request.getApplicationsToDelete() != null && request.getApplicationsToDelete().length > 0) {
            resourcePermissionService.revokePermission(location, Subject.APPLICATION, request.getApplicationsToDelete());
        }
        if (request.getEnvironmentsToDelete() != null && request.getEnvironmentsToDelete().length > 0) {
            resourcePermissionService.revokePermission(location, Subject.ENVIRONMENT, request.getEnvironmentsToDelete());
        }
        Set<String> envToAddSet = Sets.newHashSet();
        if (request.getEnvironmentsToAdd() != null) {
            envToAddSet.addAll(Sets.newHashSet(request.getEnvironmentsToAdd()));
        }
        if (request.getApplicationsToAdd() != null && request.getApplicationsToAdd().length > 0) {
            resourcePermissionService.grantPermission(location, Subject.APPLICATION, request.getApplicationsToAdd());
            // when an app is added, all eventual existing env authorizations are removed
            List<String> envIds = Lists.newArrayList();
            for (String applicationToAddId : request.getApplicationsToAdd()) {
                ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationToAddId);
                for (ApplicationEnvironment ae : aes) {
                    envIds.add(ae.getId());
                    envToAddSet.remove(ae.getId());
                }
            }
            if (!envIds.isEmpty()) {
                resourcePermissionService.revokePermission(location, Subject.ENVIRONMENT, envIds.toArray(new String[envIds.size()]));
            }
        }
        if (request.getEnvironmentsToAdd() != null && request.getEnvironmentsToAdd().length > 0) {
            resourcePermissionService.grantPermission(location, Subject.ENVIRONMENT, envToAddSet.toArray(new String[envToAddSet.size()]));
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * List all environments per application authorised to access the location.
     *
     * @return list of all environments per application.
     */
    @ApiOperation(value = "List all applications/environments authorized to access the location", notes = "Only user with ADMIN role can list authorized applications/environments for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<ApplicationEnvironmentAuthorizationDTO>> getAuthorizedEnvironmentsPerApplication(@PathVariable String orchestratorId,
            @PathVariable String locationId) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        Map<String, ApplicationEnvironmentAuthorizationDTO> aeaDTOsMap = Maps.newHashMap();
        if (location.getEnvironmentPermissions() != null && location.getEnvironmentPermissions().size() > 0) {

            // build the set of application ids
            Set<String> environmentApplicationIds = Sets.newHashSet();
            List<ApplicationEnvironment> environments = alienDAO.findByIds(ApplicationEnvironment.class,
                    location.getEnvironmentPermissions().keySet().toArray(new String[location.getEnvironmentPermissions().size()]));
            for (ApplicationEnvironment ae : environments) {
                environmentApplicationIds.add(ae.getApplicationId());
            }

            // retrieve the applications related to these environments
            List<Application> applications = alienDAO.findByIds(Application.class,
                    environmentApplicationIds.toArray(new String[environmentApplicationIds.size()]));

            // for each application, build a DTO
            for (Application application : applications) {
                ApplicationEnvironmentAuthorizationDTO dto = new ApplicationEnvironmentAuthorizationDTO();
                dto.setApplication(application);
                List<ApplicationEnvironment> aes = Lists.newArrayList();
                dto.setEnvironments(aes);
                aeaDTOsMap.put(application.getId(), dto);
            }

            for (ApplicationEnvironment ae : environments) {
                ApplicationEnvironmentAuthorizationDTO dto = aeaDTOsMap.get(ae.getApplicationId());
                dto.getEnvironments().add(ae);
            }
        }
        if (location.getApplicationPermissions() != null && location.getApplicationPermissions().size() > 0) {
            List<Application> applications = alienDAO.findByIds(Application.class,
                    location.getApplicationPermissions().keySet().toArray(new String[location.getApplicationPermissions().size()]));
            for (Application application : applications) {
                ApplicationEnvironmentAuthorizationDTO dto = aeaDTOsMap.get(application.getId());
                if (dto == null) {
                    dto = new ApplicationEnvironmentAuthorizationDTO();
                    dto.setApplication(application);
                    aeaDTOsMap.put(application.getId(), dto);
                } else {
                    // the application has detailed environment authorizations but the whole application authorization has precedence.
                    dto.setEnvironments(null);
                }
            }
        }

        List<ApplicationEnvironmentAuthorizationDTO> result = Lists.newArrayList(aeaDTOsMap.values());
        return RestResponseBuilder.<List<ApplicationEnvironmentAuthorizationDTO>> builder().data(result).build();
    }

}
