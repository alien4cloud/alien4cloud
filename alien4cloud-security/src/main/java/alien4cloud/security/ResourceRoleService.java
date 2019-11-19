package alien4cloud.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.lucene.search.join.ScoreMode;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.security.event.GroupDeletedEvent;
import alien4cloud.security.event.UserDeletedEvent;
import alien4cloud.utils.TypeScanner;

/**
 * Handle groups roles CRUD on any resource implementing {@link ISecuredResource}
 *
 * @author mourouvi
 *
 */

@Service
public class ResourceRoleService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Add a role to a specific user on a secured resource
     *
     * @param resource on which to add user role
     * @param username for who we want to add a role
     * @param role to add to a specific user for this resource
     */
    public void addUserRole(ISecuredResource resource, String username, String role) {

        validateResource(resource);
        Map<String, Set<String>> userRolesMap = resource.getUserRoles();

        if (userRolesMap == null) {
            userRolesMap = Maps.newHashMap();
            resource.setUserRoles(userRolesMap);
        }

        // Perform some verification and format the role
        role = formatRole(resource, role);
        Set<String> userRoles = userRolesMap.get(username);
        if (userRoles == null) {
            userRoles = Sets.newHashSet();
            userRolesMap.put(username, userRoles);
        }
        if (userRoles.add(role)) {
            // Only save resource if the role to be added does not exist
            alienDAO.save(resource);
        }

    }

    /**
     * Remove a role to a specific user on a resource
     *
     * @param resource on which to remove user role
     * @param username for who we want to remove a role
     * @param role to remove to a specific user for this resource
     */
    public void removeUserRole(ISecuredResource resource, String username, String role) {

        validateResource(resource);
        Map<String, Set<String>> userRolesMap = resource.getUserRoles();

        if (userRolesMap != null) {

            Set<String> userRoles = userRolesMap.get(username);
            if (userRoles != null) {
                // Perform some verification and format the role
                role = formatRole(resource, role);
                if (userRoles.remove(role)) {
                    if (userRoles.isEmpty()) {
                        // If an user does not have any more role, we remove it from the resource
                        if (userRolesMap.remove(username) != null) {
                            if (userRolesMap.isEmpty()) {
                                resource.setUserRoles(null);
                            }
                        }
                    }
                    // Only save resource if we could really remove a role
                    alienDAO.save(resource);
                }
            }
        }
    }

    /**
     * Add a role for a group on a resource
     *
     * @param resource on which to add group role
     * @param groupId on which we want to add a role
     * @param role to add to a specific group for this resource
     */
    public void addGroupRole(ISecuredResource resource, String groupId, String role) {

        validateResource(resource);
        Map<String, Set<String>> groupRolesMap = resource.getGroupRoles();
        if (groupRolesMap == null) {
            groupRolesMap = Maps.newHashMap();
            resource.setGroupRoles(groupRolesMap);
        }
        Set<String> groupRoles = groupRolesMap.get(groupId);
        if (groupRoles == null) {
            groupRoles = Sets.newHashSet();
            groupRolesMap.put(groupId, groupRoles);
        }
        if (groupRoles.add(role)) {
            // Only save group if the role to be added does not exist
            alienDAO.save(resource);
        }
    }

    /**
     * Remove a role for a group on a resource
     *
     * @param resource on which to remove group role
     * @param groupId on which we want to add a role
     * @param role to add to a specific group for this resource
     */
    public void removeGroupRole(ISecuredResource resource, String groupId, String role) {

        validateResource(resource);
        Map<String, Set<String>> groupRolesMap = resource.getGroupRoles();

        if (groupRolesMap != null) {

            Set<String> groupRoles = groupRolesMap.get(groupId);
            if (groupRoles != null) {
                // Perform some verification and format the role
                role = formatRole(resource, role);
                if (groupRoles.remove(role)) {
                    if (groupRoles.isEmpty()) {
                        // If a group do not have any more role, we remove it from the resource
                        if (groupRolesMap.remove(groupId) != null) {
                            if (groupRolesMap.isEmpty()) {
                                resource.setGroupRoles(null);
                            }
                        }
                    }
                    // Only save resource if we could really remove a role
                    alienDAO.save(resource);
                }
            }
        }
    }

    /**
     * Clean the role string
     *
     * @param resource The resource for which to format a role string.
     * @param role The role string to check and format.
     * @return The formatted and checked role string. An exception is thrown in case the role is not a valid role for the given resource.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String formatRole(ISecuredResource resource, String role) {
        if (role == null || role.toString().trim().isEmpty()) {
            throw new NotFoundException("Resource Role [" + role + "] is empty");
        }

        String goodRoleToAdd = role.toString().toUpperCase();
        try {
            Class enumClass = resource.roleEnum();
            Enum.valueOf(enumClass, goodRoleToAdd);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Resource role [" + role + "] cannot be found", e);
        }
        return goodRoleToAdd;
    }

    private void validateResource(ISecuredResource resource) {
        if (resource == null) {
            throw new NotFoundException("The target resource on which we want to update group/user role cannot be found");
        }
    }

    /**
     * Delete a groupRoles entry (groupId) in all ISecuredResource object
     *
     * @param groupId group id to remove in groupRoles
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void deleteGroupRoles(String groupId) throws ClassNotFoundException, IOException {
        QueryBuilder resourceFilter = QueryBuilders.nestedQuery("groupRoles", QueryBuilders.termQuery("groupRoles.key", groupId), ScoreMode.None);
        deleteRoles(resourceFilter, groupId, new DeleteRoleVisitor() {
            @Override
            public void deleteRoleOfOwner(Object[] securedResources, String owner) {
                deleteRoleOfGroup(securedResources, owner);
            }
        });
    }

    /**
     * Delete a userRoles entry (userId) in all ISecuredResource object
     *
     * @param userId user id (username) to remove in userRoles
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void deleteUserRoles(String userId) throws ClassNotFoundException, IOException {
        QueryBuilder resourceFilter = QueryBuilders.nestedQuery("userRoles", QueryBuilders.termQuery("userRoles.key", userId), ScoreMode.None);
        deleteRoles(resourceFilter, userId, new DeleteRoleVisitor() {
            @Override
            public void deleteRoleOfOwner(Object[] securedResources, String owner) {
                deleteRoleOfUser(securedResources, owner);
            }
        });
    }

    private void deleteRoles(QueryBuilder appFilter, String ownerId, DeleteRoleVisitor deleteRoleVisitor) throws IOException, ClassNotFoundException {
        int from = 0;
        long totalResult;

        Set<String> indices = Sets.newHashSet();
        Set<Class<?>> classes = TypeScanner.scanTypes("alien4cloud.model", ISecuredResource.class);

        for (Class<?> clazz : classes) {
            indices.addAll(Arrays.asList(alienDAO.getIndexForType(clazz)));
        }

        do {
            GetMultipleDataResult<Object> result = alienDAO.search(indices.toArray(new String[indices.size()]), classes.toArray(new Class<?>[classes.size()]),
                    null, null, appFilter, null, from, 20);
            deleteRoleVisitor.deleteRoleOfOwner(result.getData(), ownerId);
            from += result.getData().length;
            totalResult = result.getTotalResults();
        } while (from < totalResult);
    }

    private void deleteRoleOfGroup(Object[] securedResources, String groupId) {
        for (Object securedResource : securedResources) {
            // Only remove in class implementing ISecuredResource
            ISecuredResource resource = (ISecuredResource) securedResource;
            if (resource.getGroupRoles().remove(groupId) != null) {
                if (resource.getGroupRoles().isEmpty()) {
                    resource.setGroupRoles(null);
                }
                alienDAO.save(resource);
            }
        }
    }

    private void deleteRoleOfUser(Object[] securedResources, String userId) {
        for (Object securedResource : securedResources) {
            // Only remove in class implementing ISecuredResource
            ISecuredResource resource = (ISecuredResource) securedResource;
            if (resource.getUserRoles().remove(userId) != null) {
                if (resource.getUserRoles().isEmpty()) {
                    resource.setUserRoles(null);
                }
                alienDAO.save(resource);
            }
        }
    }

    private static interface DeleteRoleVisitor {

        void deleteRoleOfOwner(Object[] securedResources, String owner);
    }

    @EventListener
    public void userDeletedEventListener(UserDeletedEvent event) throws IOException, ClassNotFoundException {
        deleteUserRoles(event.getUser().getUsername());
    }

    @EventListener
    public void groupDeletedEventListener(GroupDeletedEvent event) throws IOException, ClassNotFoundException {
        deleteGroupRoles(event.getGroup().getId());
    }
}
