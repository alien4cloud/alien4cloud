package alien4cloud.security;

import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import alien4cloud.Constants;
import alien4cloud.security.groups.Group;
import alien4cloud.security.groups.IAlienGroupDao;

import com.google.common.collect.Sets;

/**
 * Applications and topologies concerns
 */
@Slf4j
@Component
public final class AuthorizationUtil {

    private static IAlienGroupDao alienGroupDao;

    @Autowired
    public void setAlienGroupDao(IAlienGroupDao alienGroupDao) {
        this.alienGroupDao = alienGroupDao;
    }

    private AuthorizationUtil() {
    }

    /**
     * Check that the user has one of the requested rights for the given application
     *
     * @param resource any with userRoles and groupRoles maps
     * @param expectedRoles any role binded on secured resource
     */
    public static void checkAuthorizationForApplication(ISecuredResource resource, IResourceRoles... expectedRoles) {
        if (!hasAuthorizationForApplication(resource, expectedRoles)) {
            throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                    + "> has no authorization to perform the requested operation on this application.");
        }
    }

    /**
     * Check that the user has one of the requested rights for the given cloud
     *
     * @param resource
     * @param expectedRoles
     */
    public static void checkAuthorizationForCloud(ISecuredResource resource, IResourceRoles... expectedRoles) {
        if (!hasAuthorizationForCloud(resource, expectedRoles)) {
            throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                    + "> has no authorization to perform the requested operation on this cloud.");
        }
    }

    public static boolean hasAuthorizationForApplication(ISecuredResource resource, IResourceRoles... expectedRoles) {
        return hasAuthorization(getCurrentUser(), resource, ApplicationRole.APPLICATION_MANAGER, expectedRoles);
    }

    public static boolean hasAuthorizationForCloud(ISecuredResource resource, IResourceRoles... expectedRoles) {
        return hasAuthorization(getCurrentUser(), resource, CloudRole.CLOUD_DEPLOYER, expectedRoles);
    }

    /**
     * Check that the current user has the one of the requested role and throw an {@link AccessDeniedException} in case the user has none of the expected roles.
     *
     * @param expectedRoles The list of roles that are required for the user (must have one of them).
     */
    public static void checkHasOneRoleIn(Role... expectedRoles) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        checkHasOneRoleIn(auth, expectedRoles);
    }

    /**
     * Check that the current user has the one of the requested role and throw an {@link AccessDeniedException} in case the user has none of the expected roles.
     *
     * @param auth authentication information
     * @param expectedRoles The list of roles that are required for the user (must have one of them).
     */
    public static void checkHasOneRoleIn(Authentication auth, Role... expectedRoles) {
        if (hasOneRoleIn(auth, expectedRoles)) {
            return;
        }
        throw new AccessDeniedException("user <" + auth.getName() + "> has no authorization to perform the requested operation.");
    }

    /**
     * True when authorities contains at least one of the expectedRoles or Role.ADMIN
     *
     * @param expectedRoles The list of expected roles.
     * @return true if the user has the requested role or if user is ADMIN.
     */
    public static boolean hasOneRoleIn(Role... expectedRoles) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return hasOneRoleIn(auth, expectedRoles);
    }

    /**
     * True when authorities contains at least one of the expectedRoles or Role.ADMIN
     *
     * @param auth authentication information
     * @param expectedRoles The list of expected roles.
     * @return true if the user has the requested role or if user is ADMIN.
     */
    public static boolean hasOneRoleIn(Authentication auth, Role... expectedRoles) {
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority(Role.ADMIN.toString()))) {
            return true;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            for (Role role : expectedRoles) {
                if (role.toString().equals(authority.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add a filter that check for authorizations on resources
     * Takes also in account the ALL_USER group
     */
    public static FilterBuilder getResourceAuthorizationFilters() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        FilterBuilder filterBuilder = null;
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority(Role.ADMIN.toString()))) {
            return filterBuilder;
        } else {
            User user = (User) auth.getPrincipal();
            String groupId = getAllUsersGroupId();
            if (user.getGroups() != null && !user.getGroups().isEmpty()) {
                filterBuilder = FilterBuilders.boolFilter()
                        .should(FilterBuilders.nestedFilter("userRoles", FilterBuilders.termFilter("userRoles.key", auth.getName())))
                        .should(FilterBuilders.nestedFilter("groupRoles", FilterBuilders.inFilter("groupRoles.key", user.getGroups().toArray())));
            } else {
                filterBuilder = FilterBuilders.nestedFilter("userRoles", FilterBuilders.termFilter("userRoles.key", auth.getName()));
            }
            // add ALL_USERS group as OR filter
            filterBuilder = FilterBuilders.orFilter(filterBuilder,
                    FilterBuilders.nestedFilter("groupRoles", FilterBuilders.inFilter("groupRoles.key", groupId)));
        }
        return filterBuilder;
    }

    /**
     * Get current logged in user
     *
     * @return logged in user
     */
    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return user;
    }

    /**
     * Return the role that the current user has over the given resource
     *
     * @param user the user to check for
     * @param resource the resource to check for
     * @return all roles on the resource if any, empty set otherwise
     */
    public static Set<String> getRolesForResource(User user, ISecuredResource resource) {
        Set<String> allRoles = Sets.newHashSet();
        if (resource.getUserRoles() != null) {
            Set<String> userRoles = resource.getUserRoles().get(user.getUsername());
            if (userRoles != null && !userRoles.isEmpty()) {
                allRoles.addAll(userRoles);
            }
        }
        Set<String> groups = user.getGroups();
        Map<String, Set<String>> groupRolesMap = resource.getGroupRoles();
        if (groups != null && !groups.isEmpty() && groupRolesMap != null && !groupRolesMap.isEmpty()) {
            for (String group : groups) {
                Set<String> groupRoles = groupRolesMap.get(group);
                if (groupRoles == null || groupRoles.isEmpty()) {
                    continue;
                } else {
                    allRoles.addAll(groupRoles);
                }
            }
        }
        return allRoles;
    }

    /**
     * Return all the roles of an user (Alien 4 Cloud's roles)
     *
     * @param user
     * @return all user's A4C roles
     */
    private static Set<String> getRoles(User user) {
        Set<String> allRoles = Sets.newHashSet();
        String[] userRoles = user.getRoles();
        Set<String> groupRoles = user.getGroupRoles();
        if (userRoles != null && userRoles.length > 0) {
            allRoles.addAll(Sets.newHashSet(userRoles));
        }
        if (groupRoles != null && !groupRoles.isEmpty()) {
            allRoles.addAll(groupRoles);
        }
        return allRoles;
    }

    private static boolean hasAtLeastOneRole(Set<String> actualRoles, IResourceRoles adminRole, IResourceRoles... expectedRoles) {
        if (actualRoles == null || actualRoles.isEmpty()) {
            return false;
        }
        // Check admin role => true when got adminRole
        if (actualRoles.contains(adminRole.toString())) {
            return true;
        }
        for (IResourceRoles expectedRole : expectedRoles) {
            if (actualRoles.contains(expectedRole.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user is authorized
     *
     * @param user the user to check for
     * @param resource the resource to check for
     * @param resourceAdminRole the role which has the god/admin right on the resource
     * @param expectedRoles the role that the user is expected to have in order to have access to the resources
     * @throws org.springframework.security.access.AccessDeniedException if user is not authorized
     */
    public static void checkAuthorization(User user, ISecuredResource resource, IResourceRoles resourceAdminRole, IResourceRoles... expectedRoles) {
        if (!hasAuthorization(user, resource, resourceAdminRole, expectedRoles)) {
            throw new AccessDeniedException("user <" + user.getUsername() + "> has no authorization to perform the requested operation on this cloud.");
        }
    }

    /**
     * Check if user is authorized
     *
     * @param user the user to check for
     * @param resource the resource to check for
     * @param resourceAdminRole the role which has the god/admin right on the resource
     * @param expectedRoles the role that the user is expected to have in order to have access to the resources
     * @return true if user has access, false otherwise
     */
    public static boolean hasAuthorization(User user, ISecuredResource resource, IResourceRoles resourceAdminRole, IResourceRoles... expectedRoles) {
        if (resource == null) {
            // Trick for topology's template
            return true;
        }
        if (hasAllUsersDefaultGroup(resource)) {
            return true;
        }
        Set<String> alienRoles = getRoles(user);
        // With ADMIN role, all rights
        if (alienRoles.contains(Role.ADMIN.toString())) {
            return true;
        }
        Set<String> appRoles = getRolesForResource(user, resource);
        return hasAtLeastOneRole(appRoles, resourceAdminRole, expectedRoles);
    }

    /**
     * True when the defaultGroupName is present on the given resource
     * 
     * @param resource
     * @return boolean
     */
    public static boolean hasAllUsersDefaultGroup(ISecuredResource resource) {
        Map<String, Set<String>> groupRoles = resource.getGroupRoles();
        if (groupRoles != null) {
            String groupName = null;
            for (String groupId : groupRoles.keySet()) {
                groupName = alienGroupDao.find(groupId).getName();
                if (groupName.equals(Constants.GROUP_NAME_ALL_USERS)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Recover the group id for the default ALL_USER group
     * 
     * @return
     */
    private static String getAllUsersGroupId() {
        Group group = alienGroupDao.findByName(Constants.GROUP_NAME_ALL_USERS);
        if (group == null) {
            log.info("Default all users group <{}> not found", Constants.GROUP_NAME_ALL_USERS);
            return "";
        }
        return group.getId();
    }
}
