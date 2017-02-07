package alien4cloud.rest.orchestrator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Resource;

import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.IdsFilterBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
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
import io.swagger.annotations.ApiParam;

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
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(location));
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
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(location));
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
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(location));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }

    /**
     * search users authorised to access the location.
     *
     * @return {@link RestResponse} that contains a {@link GetMultipleDataResult} of {@link UserDTO}..
     */
    // TODO consider merging this with getAuthorizedUsers
    @ApiOperation(value = "List all users authorized to access the location", notes = "Only user with ADMIN role can list authorized users to the location.")
    @RequestMapping(value = "/users/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<GetMultipleDataResult<UserDTO>> getAuthorizedUsersPaginated(@PathVariable String orchestratorId, @PathVariable String locationId,
            @ApiParam(value = "Text Query to search.") @RequestParam(required = false) String query,
            @ApiParam(value = "Query from the given i*ndex.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        if (MapUtils.isEmpty(location.getUserPermissions())) {
            return RestResponseBuilder.<GetMultipleDataResult<UserDTO>> builder().data(new GetMultipleDataResult<>()).build();
        }
        IdsFilterBuilder idFilters = FilterBuilders.idsFilter()
                .ids(location.getUserPermissions().keySet().toArray(new String[location.getUserPermissions().size()]));
        GetMultipleDataResult<User> tempResult = alienUserDao.find(query, from, size, idFilters);
        return RestResponseBuilder.<GetMultipleDataResult<UserDTO>> builder().data(UserDTO.convert(tempResult)).build();
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
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(location));
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
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(location));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
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
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(location));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }


    /**
     * search groups authorised to access the location.
     *
     * @return {@link RestResponse} that contains a {@link GetMultipleDataResult} of {@link GroupDTO}..
     */
    // TODO consider merging this with getAuthorizedGroups
    @ApiOperation(value = "List all groups authorized to access the location", notes = "Only user with ADMIN role can list authorized groups to the location.")
    @RequestMapping(value = "/groups/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<GetMultipleDataResult<GroupDTO>> getAuthorizedGroupsPaginated(@PathVariable String orchestratorId, @PathVariable String locationId,
            @ApiParam(value = "Text Query to search.") @RequestParam(required = false) String query,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        if (MapUtils.isEmpty(location.getGroupPermissions())) {
            return RestResponseBuilder.<GetMultipleDataResult<GroupDTO>> builder().data(new GetMultipleDataResult<>()).build();
        }
        IdsFilterBuilder idFilters = FilterBuilders.idsFilter()
                .ids(location.getGroupPermissions().keySet().toArray(new String[location.getGroupPermissions().size()]));
        GetMultipleDataResult<Group> tempResult = alienGroupDao.find(query, from, size, idFilters);
        return RestResponseBuilder.<GetMultipleDataResult<GroupDTO>> builder().data(GroupDTO.convert(tempResult)).build();
    }

    /*******************************************************************************************************************************
     *
     * SECURITY ON APPLICATIONS
     *
     *******************************************************************************************************************************/

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
        String[] envIds = Arrays.stream(aes).map(ae -> ae.getId()).toArray(String[]::new);
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
        List<String> envIds = Lists.newArrayList();
        if (request.getApplicationsToAdd() != null && request.getApplicationsToAdd().length > 0) {
            resourcePermissionService.grantPermission(location, Subject.APPLICATION, request.getApplicationsToAdd());
            // when an app is added, all eventual existing env authorizations are removed
            for (String applicationToAddId : request.getApplicationsToAdd()) {
                ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationToAddId);
                for (ApplicationEnvironment ae : aes) {
                    envIds.add(ae.getId());
                }
            }
            if (!envIds.isEmpty()) {
                resourcePermissionService.revokePermission(location, Subject.ENVIRONMENT, envIds.toArray(new String[envIds.size()]));
            }
        }
        if (request.getEnvironmentsToAdd() != null && request.getEnvironmentsToAdd().length > 0) {
            List<String> envToAddSet = Arrays.stream(request.getEnvironmentsToAdd()).filter(env -> !envIds.contains(env)).collect(Collectors.toList());
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
        List<Application> applicationsRelatedToEnvironment = Lists.newArrayList();
        List<ApplicationEnvironment> environments = Lists.newArrayList();
        List<Application> applications = Lists.newArrayList();

        if (location.getEnvironmentPermissions() != null && location.getEnvironmentPermissions().size() > 0) {
            environments = alienDAO.findByIds(ApplicationEnvironment.class, location.getEnvironmentPermissions().keySet().toArray(new String[location.getEnvironmentPermissions().size()]));
            Set<String> environmentApplicationIds = environments.stream().map(ae -> new String(ae.getApplicationId())).collect(Collectors.toSet());
            applicationsRelatedToEnvironment = alienDAO.findByIds(Application.class, environmentApplicationIds.toArray(new String[environmentApplicationIds.size()]));
        }
        if (location.getApplicationPermissions() != null && location.getApplicationPermissions().size() > 0) {
            applications = alienDAO.findByIds(Application.class, location.getApplicationPermissions().keySet().toArray(new String[location.getApplicationPermissions().size()]));
        }

        List<ApplicationEnvironmentAuthorizationDTO> result = ApplicationEnvironmentAuthorizationDTO.buildDTOs(applicationsRelatedToEnvironment, environments, applications);
        return RestResponseBuilder.<List<ApplicationEnvironmentAuthorizationDTO>> builder().data(result).build();
    }

    /**
     * search applications/environments authorised to access the location.
     *
     * @return {@link RestResponse} that contains a {@link GetMultipleDataResult} of {@link GroupDTO}..
     */
    @ApiOperation(value = "List all groups authorized to access the location", notes = "Only user with ADMIN role can list authorized applications/environments to the location.")
    @RequestMapping(value = "/applications/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<GetMultipleDataResult<Application>> getAuthorizedEnvironmentsPerApplicationPaginated(@PathVariable String orchestratorId, @PathVariable String locationId,
                                                                                      @RequestParam(required = false, defaultValue = "false") boolean connectedOnly,
                                                                                      @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
                                                                                      @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        Set<String> allApplicationIds = Sets.newHashSet();
        if (location.getEnvironmentPermissions() != null && location.getEnvironmentPermissions().size() > 0) {
            List<ApplicationEnvironment> environments = alienDAO.findByIds(ApplicationEnvironment.class, location.getEnvironmentPermissions().keySet().toArray(new String[location.getEnvironmentPermissions().size()]));
            allApplicationIds.addAll(environments.stream().map(ae -> new String(ae.getApplicationId())).collect(Collectors.toSet()));
        }
        if (location.getApplicationPermissions() != null && location.getApplicationPermissions().size() > 0) {
            allApplicationIds.addAll(location.getApplicationPermissions().keySet());
        }
        int to = (from + size < allApplicationIds.size()) ? from + size : allApplicationIds.size();
        List<String> ids = Lists.newArrayList(allApplicationIds);
        ids = IntStream.range(from, to).mapToObj(ids::get).collect(Collectors.toList());
        IdsFilterBuilder idFilters = FilterBuilders.idsFilter().ids(ids.toArray(new String[ids.size()]));
        GetMultipleDataResult<Application> tempResult = alienDAO.search(Application.class, null, null,  idFilters, null, from,  to,  "id",  false);
        return RestResponseBuilder.<GetMultipleDataResult<Application>> builder().data(tempResult).build();
    }
}