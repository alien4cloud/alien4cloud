package alien4cloud.security;

import java.util.Map;
import java.util.Set;

/**
 * Objects that implements {@link ISecuredResource} provides management operations for users and groups.
 */
public interface ISecuredResource {
    Map<String, Set<String>> getUserRoles();

    void setUserRoles(Map<String, Set<String>> userRolesMap);

    Map<String, Set<String>> getGroupRoles();

    void setGroupRoles(Map<String, Set<String>> userRolesMap);
}