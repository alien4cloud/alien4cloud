package alien4cloud.rest.orchestrator;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import org.elasticsearch.common.collect.Lists;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AbstractSecurityEnabledResource;
import alien4cloud.security.ResourcePermissionService;
import alien4cloud.security.Subject;
import alien4cloud.security.groups.IAlienGroupDao;
import alien4cloud.security.model.Group;
import alien4cloud.security.users.IAlienUserDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Resource
    private IAlienUserDao alienUserDao;
    @Resource
    private IAlienGroupDao alienGroupDao;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ResourcePermissionService resourcePermissionService;

    private List<GroupDTO> getAuthorizedGroups(AbstractSecurityEnabledResource resource) {
        List<GroupDTO> groupDTOS = Lists.newArrayList();
        if (resource.getGroupPermissions() != null && resource.getGroupPermissions().size() > 0) {
            List<Group> groups = alienGroupDao.find(resource.getGroupPermissions().keySet().toArray(new String[resource.getGroupPermissions().size()]));
            groups.sort(Comparator.comparing(Group::getName));
            groupDTOS = groups.stream().map(group -> new GroupDTO(group.getId(), group.getName(), group.getEmail(), group.getDescription()))
                    .collect(Collectors.toList());
        }
        return groupDTOS;
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
        return RestResponseBuilder.<List<GroupDTO>> builder().data(getAuthorizedGroups(resourceTemplate)).build();
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
        return RestResponseBuilder.<List<GroupDTO>> builder().data(getAuthorizedGroups(resourceTemplate)).build();
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
        return RestResponseBuilder.<List<GroupDTO>> builder().data(getAuthorizedGroups(resourceTemplate)).build();
    }

}
