package alien4cloud.rest.orchestrator;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
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
        // TODO check that provided users are authorized on this location before. what to is one of them is not ?
        // Location location = locationService.getLocation(orchestratorId, locationId);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.grantPermission(resourceTemplate, Subject.USER, userNames);
        List<UserDTO> users = LocationSecurityController.convertListUserToListUserDTO(resourcePermissionService.getAuthorizedUsers(resourceTemplate));
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
        // TODO check that provided users are authorized on this location before. what to is one of them is not ?
        // Location location = locationService.getLocation(orchestratorId, locationId);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.revokePermission(resourceTemplate, Subject.USER, username);
        List<UserDTO> users = LocationSecurityController.convertListUserToListUserDTO(resourcePermissionService.getAuthorizedUsers(resourceTemplate));
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
        List<UserDTO> users = LocationSecurityController.convertListUserToListUserDTO(resourcePermissionService.getAuthorizedUsers(resourceTemplate));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();    }


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
                                                                         @PathVariable String resourceId, @RequestBody String[] groupIds) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        locationSecurityService.checkAuthorisation(location, (String) null);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.grantPermission(resourceTemplate, Subject.GROUP, groupIds);
        List<GroupDTO> groups = LocationSecurityController.convertListGroupToListGroupDTO(resourcePermissionService.getAuthorizedGroups(resourceTemplate));
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
                                                                       @PathVariable String resourceId, @PathVariable String groupId) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        locationSecurityService.checkAuthorisation(location, (String) null);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        resourcePermissionService.revokePermission(resourceTemplate, Subject.GROUP, groupId);
        List<GroupDTO> groups = LocationSecurityController.convertListGroupToListGroupDTO(resourcePermissionService.getAuthorizedGroups(resourceTemplate));
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
    public RestResponse<List<GroupDTO>> getAuthorizedGroups(@PathVariable String orchestratorId, @PathVariable String locationId, @PathVariable String resourceId) {
        Location location = locationService.getLocation(orchestratorId, locationId);
        locationSecurityService.checkAuthorisation(location, (String) null);
        LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
        List<GroupDTO> groups = LocationSecurityController.convertListGroupToListGroupDTO(resourcePermissionService.getAuthorizedGroups(resourceTemplate));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }


}
