package alien4cloud.rest.orchestrator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.elasticsearch.common.collect.Lists;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationDTO;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationUpdateRequest;
import alien4cloud.rest.orchestrator.model.GroupDTO;
import alien4cloud.rest.orchestrator.model.UserDTO;
import alien4cloud.security.ResourcePermissionService;
import alien4cloud.security.Subject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({ "/rest/orchestrators/{orchestratorId}/locations/{locationId}/resources/{resourceId}/security/",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/resources/{resourceId}/security/",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/resources/{resourceId}/security/" })
@Api(value = "", description = "Location resource security operations")
public class LocationResourcesSecurityController {
    @Resource
    private LocationService locationService;
    @Resource
    private LocationSecurityService locationSecurityService;
    @Resource
    private ILocationResourceService locationResourceService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ResourcePermissionService resourcePermissionService;

    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;


    private void checkAuthorization(Location resource, Subject subject, String[] names) {
        Map<Subject, Set<String>> subjectsMap = new HashMap<>();
        subjectsMap.put(subject, Sets.newHashSet(names));
        // TODO improve this so that we know exactly who doesn't have the permission
        if (!resourcePermissionService.allHavePermission(resource, subjectsMap)) {
            throw new AccessDeniedException("At least one of the current <" + names + "> <" + subject + "> has no authorization on location <" + resource.getName() + "> to perform the requested operation.");
        }
    }

    /*******************************************************************************************************************************
     *
     * SECURITY ON USERS
     *
     *******************************************************************************************************************************/

    /**
     * Grant access to the location resoure to the user (deploy on the location)
     *
     * @param locationId The location's id.
     * @param resourceId The location resource's id.
     * @param userNames The authorized users.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the location's resource to the users, send back the new authorised users list", notes = "Only user with ADMIN role can grant access to another users.")
    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<UserDTO>> grantAccessToUsers(@PathVariable String orchestratorId, @PathVariable String locationId,
                                                                    @PathVariable String resourceId, @RequestBody String[] userNames) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        checkAuthorization(location, Subject.USER, userNames);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.grantPermission(resourceTemplate, Subject.USER, userNames);
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(resourceTemplate));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }

    /**
     * Revoke the user's authorisation to access a location resource
     *
     * @param locationId The id of the location.
     * @param username The authorized user.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the user's authorisation to access a location resource, send back the new authorised users list", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<UserDTO>> revokeUserAccess(@PathVariable String orchestratorId, @PathVariable String locationId,
                                                                  @PathVariable String resourceId, @PathVariable String username) {
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.revokePermission(resourceTemplate, Subject.USER, username);
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(resourceTemplate));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }

    /**
     * List all users authorised to access the location resource.
     *
     * @return list of all authorized users.
     */
    @ApiOperation(value = "List all users authorized to access the location resource", notes = "Only user with ADMIN role can list authorized users to the location.")
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<UserDTO>> getAuthorizedUsers(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String resourceId) {
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(resourceTemplate));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();    }


    /*******************************************************************************************************************************
     *
     * SECURITY ON GROUPS
     *
     *******************************************************************************************************************************/

    /**
     * Grant access to the location resource to the groups
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
                                                                         @PathVariable String resourceId, @RequestBody String[] groupIds) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        checkAuthorization(location, Subject.GROUP, groupIds);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.grantPermission(resourceTemplate, Subject.GROUP, groupIds);
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(resourceTemplate));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }

    /**
     * Revoke the group's authorisation to access the location resource
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
                                                                       @PathVariable String resourceId, @PathVariable String groupId) {
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.revokePermission(resourceTemplate, Subject.GROUP, groupId);
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(resourceTemplate));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }

    /**
     * List all groups authorised to access the location resource.
     *
     * @return list of all authorised groups.
     */
    @ApiOperation(value = "List all groups authorized to access the location", notes = "Only user with ADMIN role can list authorized groups to the location.")
    @RequestMapping(value = "/groups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<GroupDTO>> getAuthorizedGroups(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String resourceId) {
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(resourceTemplate));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }


    /*******************************************************************************************************************************
     *
     * SECURITY ON APPLICATIONS
     *
     *******************************************************************************************************************************/

    /**
     * Revoke the application's authorisation to access the location resource (including all related environments).
     *
     * @param locationId The id of the location.
     * @param applicationId The authorized application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the application's authorisation to access the location resource", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/applications/{applicationId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<Void> revokeApplicationAccess(@PathVariable String orchestratorId, @PathVariable String locationId,
                                                                   @PathVariable String applicationId, @PathVariable String resourceId) {
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.revokePermission(resourceTemplate, Subject.APPLICATION, applicationId);
        // remove all environments related to this application
        ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationId);
        String[] envIds = Arrays.stream(aes).map(ae -> ae.getId()).toArray(String[]::new);
        resourcePermissionService.revokePermission(resourceTemplate, Subject.ENVIRONMENT, envIds);
        return RestResponseBuilder.<Void>builder().build();
    }

    private void checkAllAuthorizationsForApplicationsAndEnvironments(ApplicationEnvironmentAuthorizationUpdateRequest request, Location location) {
        if (request.getApplicationsToDelete() != null && request.getApplicationsToDelete().length > 0) {
            checkAuthorization(location, Subject.APPLICATION, request.getApplicationsToDelete());
        }
        if (request.getEnvironmentsToDelete() != null && request.getEnvironmentsToDelete().length > 0) {
            checkAuthorization(location, Subject.ENVIRONMENT, request.getEnvironmentsToDelete());
        }
        if (request.getApplicationsToAdd() != null && request.getApplicationsToAdd().length > 0) {
            checkAuthorization(location, Subject.APPLICATION, request.getApplicationsToAdd());
        }
        if (request.getEnvironmentsToAdd() != null && request.getEnvironmentsToAdd().length > 0) {
            checkAuthorization(location, Subject.ENVIRONMENT, request.getEnvironmentsToAdd());
        }
    }

    /**
     * Update applications/environments authorized to access the location resource.
     */
    @ApiOperation(value = "Update applications/environments authorized to access the location resource", notes = "Only user with ADMIN role can update authorized applications/environments for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized RestResponse<Void> updateAuthorizedEnvironmentsPerApplication(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String resourceId,
                                                                                      @RequestBody ApplicationEnvironmentAuthorizationUpdateRequest request) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        checkAllAuthorizationsForApplicationsAndEnvironments(request, location);

        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        if (request.getApplicationsToDelete() != null && request.getApplicationsToDelete().length > 0) {
            resourcePermissionService.revokePermission(resourceTemplate, Subject.APPLICATION, request.getApplicationsToDelete());
        }
        if (request.getEnvironmentsToDelete() != null && request.getEnvironmentsToDelete().length > 0) {
            resourcePermissionService.revokePermission(resourceTemplate, Subject.ENVIRONMENT, request.getEnvironmentsToDelete());
        }
        List<String> envIds = Lists.newArrayList();
        if (request.getApplicationsToAdd() != null && request.getApplicationsToAdd().length > 0) {
            resourcePermissionService.grantPermission(resourceTemplate, Subject.APPLICATION, request.getApplicationsToAdd());
            // when an app is added, all eventual existing env authorizations are removed
            for (String applicationToAddId : request.getApplicationsToAdd()) {
                ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationToAddId);
                for (ApplicationEnvironment ae : aes) {
                    envIds.add(ae.getId());
                }
            }
            if (!envIds.isEmpty()) {
                resourcePermissionService.revokePermission(resourceTemplate, Subject.ENVIRONMENT, envIds.toArray(new String[envIds.size()]));
            }
        }
        if (request.getEnvironmentsToAdd() != null && request.getEnvironmentsToAdd().length > 0) {
            List<String> envToAddSet = Arrays.stream(request.getEnvironmentsToAdd()).filter(env -> !envIds.contains(env)).collect(Collectors.toList());
            resourcePermissionService.grantPermission(resourceTemplate, Subject.ENVIRONMENT, envToAddSet.toArray(new String[envToAddSet.size()]));
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * List all environments per application authorised to access the location resource.
     *
     * @return list of all environments per application.
     */
    @ApiOperation(value = "List all applications/environments authorized to access the location resource", notes = "Only user with ADMIN role can list authorized applications/environments for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<ApplicationEnvironmentAuthorizationDTO>> getAuthorizedEnvironmentsPerApplication(@PathVariable String orchestratorId,
                                                                                                              @PathVariable String locationId, @PathVariable String resourceId) {
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        Map<String, ApplicationEnvironmentAuthorizationDTO> aeaDTOsMap = Maps.newHashMap();
        if (resourceTemplate.getEnvironmentPermissions() != null && resourceTemplate.getEnvironmentPermissions().size() > 0) {

            // build the set of application ids
            List<ApplicationEnvironment> environments = alienDAO.findByIds(ApplicationEnvironment.class,
                    resourceTemplate.getEnvironmentPermissions().keySet().toArray(new String[resourceTemplate.getEnvironmentPermissions().size()]));
            Set<String> environmentApplicationIds = environments.stream().map(ae -> new String(ae.getApplicationId())).collect(Collectors.toSet());

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
        if (resourceTemplate.getApplicationPermissions() != null && resourceTemplate.getApplicationPermissions().size() > 0) {
            List<Application> applications = alienDAO.findByIds(Application.class,
                    resourceTemplate.getApplicationPermissions().keySet().toArray(new String[resourceTemplate.getApplicationPermissions().size()]));
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
