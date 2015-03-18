package alien4cloud.security;

import java.util.Map;
import java.util.Set;

/**
 * Objects that implements {@link ISecuredResource} provides management operations for users and groups.
 */
public interface ISecuredResource {
    /**
     * Get the enum that contains possible roles for the given resource.
     *
     * @return The map of valid roles for the resource.
     */
    <T extends Enum<T>> Class<T> roleEnum();

    /**
     * Get a map of the user roles for the secured resource.
     *
     * @return Return a map of userId => set of roles.
     */
    Map<String, Set<String>> getUserRoles();

    /**
     * Update a map of the user roles for the secured resource.
     *
     * @param userRolesMap Map of userId => set of roles.
     */
    void setUserRoles(Map<String, Set<String>> userRolesMap);

    /**
     * Get a map of the group roles for the secured resource.
     *
     * @return Return a map of groupId => set of roles.
     */
    Map<String, Set<String>> getGroupRoles();

    /**
     * Update a map of the group roles for the secured resource.
     *
     * @param userRolesMap Map of groupId => set of roles.
     */
    void setGroupRoles(Map<String, Set<String>> userRolesMap);
}