package alien4cloud.rest.orchestrator;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.authorization.ResourcePermissionService;
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
import alien4cloud.security.Subject;
import alien4cloud.security.groups.IAlienGroupDao;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.User;
import alien4cloud.security.users.IAlienUserDao;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        // remove all environments types related to this application
        Set<String> envTypeIds = Sets.newHashSet();
        for (String envType : location.getEnvironmentTypePermissions().keySet()) {
            if (envType.contains(applicationId)) {
                envTypeIds.add(envType);
            }
        }
        resourcePermissionService.revokePermission(location, Subject.ENVIRONMENT_TYPE, envTypeIds.toArray(new String[envTypeIds.size()]));

        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Update applications,environments and environment types authorized to access the location.
     */
    @ApiOperation(value = "Update applications,environments and environment types authorized to access the location", notes = "Only user with ADMIN role can update authorized applications,environments and environment types for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized RestResponse<Void> updateAuthorizedEnvironmentsAndEnvTypesPerApplication(@PathVariable String orchestratorId, @PathVariable String locationId,
                                                                                                 @RequestBody ApplicationEnvironmentAuthorizationUpdateRequest request) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        resourcePermissionService.revokeAuthorizedEnvironmentsPerApplication(location, request.getApplicationsToDelete(), request.getEnvironmentsToDelete(), request.getEnvironmentTypesToDelete());
        resourcePermissionService.grantAuthorizedEnvironmentsAndEnvTypesPerApplication(location, request.getApplicationsToAdd(), request.getEnvironmentsToAdd(), request.getEnvironmentTypesToAdd());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * List all environments per application authorised to access the location.
     *
     * @return list of all environments per application.
     */
    @ApiOperation(value = "List all applications,environments and environment types authorized to access the location", notes = "Only user with ADMIN role can list authorized applications,environments and environment types for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<ApplicationEnvironmentAuthorizationDTO>> getAuthorizedEnvironmentsAndEnvTypePerApplication(@PathVariable String orchestratorId,
                                                                                                                        @PathVariable String locationId) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        List<Application> applicationsRelatedToEnvironment = Lists.newArrayList();
        List<Application> applicationsRelatedToEnvironmentType = Lists.newArrayList();
        List<ApplicationEnvironment> environments = Lists.newArrayList();
        List<Application> applications = Lists.newArrayList();
        List<String> environmentTypes = Lists.newArrayList();

        if (location.getEnvironmentPermissions() != null && location.getEnvironmentPermissions().size() > 0) {
            environments = alienDAO.findByIds(ApplicationEnvironment.class, location.getEnvironmentPermissions().keySet().toArray(new String[location.getEnvironmentPermissions().size()]));
            Set<String> environmentApplicationIds = environments.stream().map(ae -> new String(ae.getApplicationId())).collect(Collectors.toSet());
            applicationsRelatedToEnvironment = alienDAO.findByIds(Application.class, environmentApplicationIds.toArray(new String[environmentApplicationIds.size()]));
        }

        if (location.getEnvironmentTypePermissions() != null && location.getEnvironmentTypePermissions().size() > 0) {
            environmentTypes.addAll(location.getEnvironmentTypePermissions().keySet());
            Set<String> environmentTypeApplicationIds = environmentTypes.stream().map(envType -> new String(envType.split(":")[0])).collect(Collectors.toSet());
            applicationsRelatedToEnvironmentType = alienDAO.findByIds(Application.class, environmentTypeApplicationIds.toArray(new String[environmentTypeApplicationIds.size()]));
        }


        if (location.getApplicationPermissions() != null && location.getApplicationPermissions().size() > 0) {
            applications = alienDAO.findByIds(Application.class, location.getApplicationPermissions().keySet().toArray(new String[location.getApplicationPermissions().size()]));
        }

        List<ApplicationEnvironmentAuthorizationDTO> result = ApplicationEnvironmentAuthorizationDTO.buildDTOs(applicationsRelatedToEnvironment, applicationsRelatedToEnvironmentType, environments, applications, environmentTypes);
        return RestResponseBuilder.<List<ApplicationEnvironmentAuthorizationDTO>> builder().data(result).build();
    }

    /**
     * search applications,environments and environment types authorised to access the location.
     *
     * @return {@link RestResponse} that contains a {@link GetMultipleDataResult} of {@link GroupDTO}..
     */
    @ApiOperation(value = "List all applications,environments and environment types authorized to access the location", notes = "Only user with ADMIN role can list authorized applications,environments and environment types to the location.")
    @RequestMapping(value = "/applications/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<GetMultipleDataResult<ApplicationEnvironmentAuthorizationDTO>> getAuthorizedEnvironmentsAndEnvTypesPerApplicationPaginated(@PathVariable String orchestratorId, @PathVariable String locationId,
                                                                                                                                                   @ApiParam(value = "Text Query to search.") @RequestParam(required = false) String query,
                                                                                                                                                   @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
                                                                                                                                                   @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        List<Application> applicationsRelatedToEnvironment = Lists.newArrayList();
        List<Application> applicationsRelatedToEnvironmentType = Lists.newArrayList();
        List<ApplicationEnvironment> environments = Lists.newArrayList();
        List<String> environmentTypes = Lists.newArrayList();
        List<Application> applications = Lists.newArrayList();

        // we get all authorized applications and environment to not favor the one of them
        if (MapUtils.isNotEmpty(location.getEnvironmentPermissions())) {
            environments = alienDAO.findByIds(ApplicationEnvironment.class, location.getEnvironmentPermissions().keySet().toArray(new String[location.getEnvironmentPermissions().size()]));
            Set<String> environmentApplicationIds = environments.stream().map(ae -> new String(ae.getApplicationId())).collect(Collectors.toSet());
            applicationsRelatedToEnvironment = alienDAO.findByIds(Application.class, environmentApplicationIds.toArray(new String[environmentApplicationIds.size()]));
        }

        if (MapUtils.isNotEmpty(location.getEnvironmentTypePermissions())) {
            environmentTypes.addAll(location.getEnvironmentTypePermissions().keySet());
            Set<String> environmentTypeApplicationIds = Sets.newHashSet();
            for (String envType : location.getEnvironmentTypePermissions().keySet()) {
                environmentTypeApplicationIds.add(envType.split(":")[0]);
            }
            applicationsRelatedToEnvironmentType = alienDAO.findByIds(Application.class, environmentTypeApplicationIds.toArray(new String[environmentTypeApplicationIds.size()]));
        }

        if (MapUtils.isNotEmpty(location.getApplicationPermissions())) {
            applications = alienDAO.findByIds(Application.class, location.getApplicationPermissions().keySet().toArray(new String[location.getApplicationPermissions().size()]));
        }
        List<ApplicationEnvironmentAuthorizationDTO> allDTOs = ApplicationEnvironmentAuthorizationDTO.buildDTOs(applicationsRelatedToEnvironment, applicationsRelatedToEnvironmentType, environments, applications, environmentTypes);
        int to = (from + size < allDTOs.size()) ? from + size : allDTOs.size();
        allDTOs = IntStream.range(from, to).mapToObj(allDTOs::get).collect(Collectors.toList());
        List<String> ids = allDTOs.stream().map(appEnvDTO -> appEnvDTO.getApplication().getId()).collect(Collectors.toList());
        IdsFilterBuilder idFilters = FilterBuilders.idsFilter().ids(ids.toArray(new String[ids.size()]));
        GetMultipleDataResult<Application> tempResult = alienDAO.search(Application.class, query, null,  idFilters, null, from,  to,  "id",  false);
        return RestResponseBuilder.<GetMultipleDataResult<ApplicationEnvironmentAuthorizationDTO>> builder().data(ApplicationEnvironmentAuthorizationDTO.convert(tempResult, allDTOs)).build();
    }
}