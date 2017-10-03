package alien4cloud.rest.orchestrator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.common.collect.Lists;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.authorization.ResourcePermissionService;
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
        locationSecurityService.grantAuthorizationOnLocationIfNecessary(location, Subject.USER, userNames);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
        // prefer using locationResourceService.saveResource so that the location update date is update.
        // This will then trigger a deployment topology update
        resourcePermissionService.grantPermission(resourceTemplate,
                (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.USER, userNames);
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
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
        resourcePermissionService.revokePermission(resourceTemplate, (resource -> locationResourceService.saveResource((LocationResourceTemplate) resource)),
                Subject.USER, username);
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
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(resourceTemplate));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }


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
        locationSecurityService.grantAuthorizationOnLocationIfNecessary(location, Subject.GROUP, groupIds);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
        resourcePermissionService.grantPermission(resourceTemplate,
                (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.GROUP, groupIds);
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
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
        resourcePermissionService.revokePermission(resourceTemplate, (resource -> locationResourceService.saveResource((LocationResourceTemplate) resource)),
                Subject.GROUP, groupId);
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
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
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
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
        resourcePermissionService.revokePermission(resourceTemplate, (resource -> locationResourceService.saveResource((LocationResourceTemplate) resource)),
                Subject.APPLICATION, applicationId);

        // remove all environments related to this application
        ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationId);
        String[] envIds = Arrays.stream(aes).map(ae -> ae.getId()).toArray(String[]::new);
        resourcePermissionService.revokePermission(resourceTemplate, (resource -> locationResourceService.saveResource((LocationResourceTemplate) resource)),
                Subject.ENVIRONMENT, envIds);

        // remove all environment types
        Set<String> envTypeIds = Sets.newHashSet();
        for (String envType : resourceTemplate.getEnvironmentTypePermissions().keySet()) {
            if (envType.split(":")[0].equals(applicationId)) {
                envTypeIds.add(envType);
            }
        }
        resourcePermissionService.revokePermission(resourceTemplate, (resource -> locationResourceService.saveResource((LocationResourceTemplate) resource)),
                Subject.ENVIRONMENT_TYPE, envTypeIds.toArray(new String[envTypeIds.size()]));


        return RestResponseBuilder.<Void>builder().build();
    }

    /**
     * Update applications,environments and environment types authorized to access the location resource.
     */
    @ApiOperation(value = "Update applications,environments and environment types authorized to access the location resource", notes = "Only user with ADMIN role can update authorized applications,environments and environment types for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized RestResponse<Void> updateAuthorizedEnvironmentsAndEnvTypePerApplication(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String resourceId,
                                                                                      @RequestBody ApplicationEnvironmentAuthorizationUpdateRequest request) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        locationSecurityService.grantAuthorizationOnLocationIfNecessary(request.getApplicationsToAdd(), request.getEnvironmentsToAdd(), request.getEnvironmentTypesToAdd(), location);

        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
        if (ArrayUtils.isNotEmpty(request.getApplicationsToDelete())) {
            resourcePermissionService.revokePermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.APPLICATION,
                    request.getApplicationsToDelete());
        }
        if (ArrayUtils.isNotEmpty(request.getEnvironmentsToDelete())) {
            resourcePermissionService.revokePermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT,
                    request.getEnvironmentsToDelete());
        }
        if (ArrayUtils.isNotEmpty(request.getEnvironmentTypesToDelete())) {
            resourcePermissionService.revokePermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT_TYPE,
                    request.getEnvironmentTypesToDelete());
        }
        Set<String> envIds = Sets.newHashSet();
        if (ArrayUtils.isNotEmpty(request.getApplicationsToAdd())) {
            resourcePermissionService.grantPermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.APPLICATION,
                    request.getApplicationsToAdd());
            // when an app is added, all eventual existing env authorizations are removed
            for (String applicationToAddId : request.getApplicationsToAdd()) {
                ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationToAddId);
                for (ApplicationEnvironment ae : aes) {
                    envIds.add(ae.getId());
                }
            }
            if (!envIds.isEmpty()) {
                resourcePermissionService.revokePermission(resourceTemplate,
                        (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT,
                        envIds.toArray(new String[envIds.size()]));
            }
        }
        if (ArrayUtils.isNotEmpty(request.getEnvironmentsToAdd())) {
            List<String> envToAddSet = Arrays.stream(request.getEnvironmentsToAdd()).filter(env -> !envIds.contains(env)).collect(Collectors.toList());
            resourcePermissionService.grantPermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT,
                    envToAddSet.toArray(new String[envToAddSet.size()]));
        }
        if (ArrayUtils.isNotEmpty(request.getEnvironmentTypesToAdd())) {
            resourcePermissionService.grantPermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT_TYPE, request.getEnvironmentTypesToAdd());
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * List all environments per application authorised to access the location resource.
     *
     * @return list of all environments per application.
     */
    @ApiOperation(value = "List all applications,environments and environment types authorized to access the location resource", notes = "Only user with ADMIN role can list authorized applications,environments and environment types for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<ApplicationEnvironmentAuthorizationDTO>> getAuthorizedEnvironmentsAndEnvTypePerApplication(@PathVariable String orchestratorId,
                                                                                                              @PathVariable String locationId, @PathVariable String resourceId) {
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(LocationResourceTemplate.class, resourceId);
        List<Application> applicationsRelatedToEnvironment = Lists.newArrayList();
        List<Application> applicationsRelatedToEnvironmentType = Lists.newArrayList();
        List<ApplicationEnvironment> environments = Lists.newArrayList();
        List<String> environmentTypes = Lists.newArrayList();
        List<Application> applications = Lists.newArrayList();

        if (MapUtils.isNotEmpty(resourceTemplate.getEnvironmentPermissions())) {
            environments = alienDAO.findByIds(ApplicationEnvironment.class, resourceTemplate.getEnvironmentPermissions().keySet().toArray(new String[resourceTemplate.getEnvironmentPermissions().size()]));
            Set<String> environmentApplicationIds = environments.stream().map(ApplicationEnvironment::getApplicationId).collect(Collectors.toSet());
            applicationsRelatedToEnvironment = alienDAO.findByIds(Application.class, environmentApplicationIds.toArray(new String[environmentApplicationIds.size()]));
        }

        if (MapUtils.isNotEmpty(resourceTemplate.getEnvironmentTypePermissions())) {
            environmentTypes.addAll(resourceTemplate.getEnvironmentTypePermissions().keySet());
            Set<String> environmentTypeApplicationIds = Sets.newHashSet();
            for (String envType : resourceTemplate.getEnvironmentTypePermissions().keySet()) {
                environmentTypeApplicationIds.add(envType.split(":")[0]);
            }
            applicationsRelatedToEnvironmentType = alienDAO.findByIds(Application.class, environmentTypeApplicationIds.toArray(new String[environmentTypeApplicationIds.size()]));
        }

        if (resourceTemplate.getApplicationPermissions() != null && resourceTemplate.getApplicationPermissions().size() > 0) {
            applications = alienDAO.findByIds(Application.class, resourceTemplate.getApplicationPermissions().keySet().toArray(new String[resourceTemplate.getApplicationPermissions().size()]));
        }

        List<ApplicationEnvironmentAuthorizationDTO> result = ApplicationEnvironmentAuthorizationDTO.buildDTOs(applicationsRelatedToEnvironment, applicationsRelatedToEnvironmentType, environments, applications, environmentTypes);
        return RestResponseBuilder.<List<ApplicationEnvironmentAuthorizationDTO>> builder().data(result).build();
    }

}
