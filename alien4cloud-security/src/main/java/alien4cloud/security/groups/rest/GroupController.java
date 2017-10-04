package alien4cloud.security.groups.rest;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.ResourceRoleService;
import alien4cloud.security.groups.GroupService;
import alien4cloud.security.groups.IAlienGroupDao;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.User;
import alien4cloud.utils.AlienConstants;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;


/**
 * GroupController allows ALIEN administrators to create, delete, update, or
 * search groups. A default internal group ({@link AlienConstants#GROUP_NAME_ALL_USERS}) is created to make some security
 * check easier. Event ADMIN role can't make CRUD operation
 * on this Group
 * 
 * @author igor ngouagna
 */
@RestController
@Slf4j
@RequestMapping({"/rest/groups", "/rest/v1/groups", "/rest/latest/groups"})
public class GroupController {
    @Resource
    private IAlienGroupDao alienGroupDao;
    @Resource
    private GroupService groupService;
    @Resource
    private ResourceRoleService resourceRoleService;

    /**
     * Create a new group in the system.
     *
     * @param request A {@link CreateGroupRequest} with information of the new group to create.
     * @return a rest {@link RestResponse} containing the id of the newly created group.
     */
    @ApiOperation(value = "Create a new group in ALIEN.")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String> create(@Valid @RequestBody CreateGroupRequest request) {
        String groupId = groupService.createGroup(request.getName(), request.getEmail(), request.getDescription(), request.getRoles(), request.getUsers());
        return RestResponseBuilder.<String> builder().data(groupId).build();
    }

    @RequestMapping(value = "/{groupId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Update a group by merging the groupUpdateRequest into the existing group")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> update(@PathVariable String groupId, @RequestBody UpdateGroupRequest groupUpdateRequest) {
        if (!isInternalAllUserGroup(groupId)) {
            groupService.updateGroup(groupId, groupUpdateRequest);
        } else {
            log.info("You can not update the group with id [ {} ] corresponding to an internal group [ {} ]", groupId, AlienConstants.GROUP_NAME_ALL_USERS);
            return RestResponseBuilder
                    .<Void> builder()
                    .data(null)
                    .error(RestErrorBuilder
                            .builder(RestErrorCode.INTERNAL_OBJECT_ERROR)
                            .message(
                                    "You can not update the group with id <" + groupId + "> corresponding to an internal group <"
                                            + AlienConstants.GROUP_NAME_ALL_USERS + ">")
                            .build())
                    .build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Get a group from it's id.
     *
     * @param groupId
     *            The unique id of the group to retrieve.
     * @return a rest {@link RestResponse} containing the group matching the
     *         requested id.
     */
    @ApiOperation(value = "Get a group based on it's id.", notes = "Returns a rest response that contains the group's details.")
    @RequestMapping(value = "/{groupId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Group> getGroup(@PathVariable String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            throw new InvalidArgumentException("Group with id <" + groupId + "> does not exist");
        }
        Group group = alienGroupDao.find(groupId);
        return RestResponseBuilder.<Group> builder().data(group).build();
    }

    /**
     * Delete a group from the store based on it's id.
     *
     * @param groupId
     *            The unique id of the group to delete.
     * @return an empty (void) rest {@link RestResponse}.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @ApiOperation(value = "Delete an existing group from the repository.")
    @RequestMapping(value = "/{groupId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> deleteGroup(@PathVariable String groupId) throws ClassNotFoundException, IOException {
        if (groupId == null || groupId.isEmpty()) {
            throw new InvalidArgumentException("Group with id <" + groupId + "> does not exist");
        }
        if (!isInternalAllUserGroup(groupId)) {
            groupService.deleteGroup(groupId);
        } else {
            log.info("You can not update the group with id [ {} ] corresponding to an internal group [ {} ]", groupId, AlienConstants.GROUP_NAME_ALL_USERS);
            return RestResponseBuilder
                    .<Void> builder()
                    .data(null)
                    .error(RestErrorBuilder
                            .builder(RestErrorCode.INTERNAL_OBJECT_ERROR)
                            .message(
                                    "You can not update the group with id <" + groupId + "> corresponding to an internal group <"
                                            + AlienConstants.GROUP_NAME_ALL_USERS + ">")
                            .build())
                    .build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Search for groups.
     *
     * @param searchRequest
     *            The request that contains parameters of the search request.
     * @return A {@link RestResponse} that contains a {@link GetMultipleDataResult} of {@link Group}.
     */
    @ApiOperation(value = "Search for user's registered in alien.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<GetMultipleDataResult> searchGroups(@RequestBody FilteredSearchRequest searchRequest) {
        GetMultipleDataResult searchResult = alienGroupDao.search(searchRequest.getQuery(), searchRequest.getFilters(), searchRequest.getFrom(),
                searchRequest.getSize());
        return RestResponseBuilder.<GetMultipleDataResult> builder().data(searchResult).build();
    }

    /**
     * Get multiple groups from their names.
     *
     * @param ids
     *            The list of ids of the groups to retrieve.
     * @return a rest {@link RestResponse} containing the group matching the
     *         given names.
     */
    @ApiOperation(value = "Get multiple groups from their ids.", notes = "Returns a rest response that contains the list of requested groups.")
    @RequestMapping(value = "/getGroups", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<Group>> getGroups(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new InvalidArgumentException("ids cannot be null or empty");
        }
        List<Group> groups = alienGroupDao.find(ids.toArray(new String[ids.size()]));
        return RestResponseBuilder.<List<Group>> builder().data(groups).build();
    }

    /**
     * Add a role to a given group.
     *
     * @param groupId
     *            The unique id of the group for which to add a role.
     * @param role
     *            The role to add to the group.
     */
    @ApiOperation(value = "Add a role to a group.")
    @RequestMapping(value = "/{groupId}/roles/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> addRoleToGroup(@PathVariable String groupId, @PathVariable String role) {
        if (groupId == null || groupId.isEmpty()) {
            throw new InvalidArgumentException("groupId cannot be null or empty");
        }
        groupService.addRoleToGroup(groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Removes a role from a given group.
     *
     * @param groupId
     *            The unique id of the group from which to remove a role.
     * @param role
     *            The role to remove from the group.
     * @return an empty (void) rest {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role from a group.")
    @RequestMapping(value = "/{groupId}/roles/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> removeRoleFromGroup(@PathVariable String groupId, @PathVariable String role) {
        if (groupId == null || groupId.isEmpty()) {
            throw new InvalidArgumentException("groupId cannot be null or empty");
        }
        groupService.removeRoleFromGroup(groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a user to a group.
     *
     * @param groupId
     *            The group in which to add the given user.
     * @param username
     *            The username of the user to add to the group.
     * @return added user
     */
    @ApiOperation(value = "Add a user to a group.")
    @RequestMapping(value = "/{groupId}/users/{username}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<User> addUserToGroup(@PathVariable String groupId, @PathVariable String username) {
        if (groupId == null || groupId.isEmpty()) {
            throw new InvalidArgumentException("groupId cannot be null or empty");
        }
        User user;
        if (!isInternalAllUserGroup(groupId)) {
            user = groupService.addUserToGroup(username, groupId);
        } else {
            log.info("You can not update the group with id [ {} ] corresponding to an internal group [ {} ]", groupId, AlienConstants.GROUP_NAME_ALL_USERS);
            return RestResponseBuilder
                    .<User> builder()
                    .data(null)
                    .error(RestErrorBuilder
                            .builder(RestErrorCode.INTERNAL_OBJECT_ERROR)
                            .message(
                                    "You can not update the group with id <" + groupId + "> corresponding to an internal group <"
                                            + AlienConstants.GROUP_NAME_ALL_USERS + ">")
                            .build())
                    .build();
        }
        return RestResponseBuilder.<User> builder().data(user).build();
    }

    /**
     * remove a user from a group.
     *
     * @param groupId
     *            The group from which to remove the given user.
     * @param username
     *            The username of the user to remove from the group.
     * @return response which contain the removed user
     */
    @ApiOperation(value = "Remove a user from a group.")
    @RequestMapping(value = "/{groupId}/users/{username}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<User> removeUserFromGroup(@PathVariable String groupId, @PathVariable String username) {
        if (groupId == null || groupId.isEmpty()) {
            throw new InvalidArgumentException("groupId cannot be null or empty");
        }
        User user;
        if (!isInternalAllUserGroup(groupId)) {
            user = groupService.removeUserFromGroup(username, groupId);
        } else {
            log.info("You can not update the group with id [ {} ] corresponding to an internal group [ {} ]", groupId, AlienConstants.GROUP_NAME_ALL_USERS);
            return RestResponseBuilder
                    .<User> builder()
                    .data(null)
                    .error(RestErrorBuilder
                            .builder(RestErrorCode.INTERNAL_OBJECT_ERROR)
                            .message(
                                    "You can not update the group with id <" + groupId + "> corresponding to an internal group <"
                                            + AlienConstants.GROUP_NAME_ALL_USERS + ">")
                            .build())
                    .build();
        }
        return RestResponseBuilder.<User> builder().data(user).build();
    }

    /**
     * Determines if the requested group is the internal one corresponding to
     * "All users"
     *
     * @param groupId the group id to verify
     * @return boolean
     */
    private boolean isInternalAllUserGroup(String groupId) {
        Group group = alienGroupDao.find(groupId);
        return group != null && AlienConstants.GROUP_NAME_ALL_USERS.equals(group.getName());
    }
}
