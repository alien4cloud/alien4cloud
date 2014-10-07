package alien4cloud.security;

import alien4cloud.exception.NotFoundException;

public enum Role implements IResourceRoles {
    ADMIN,
    APPLICATIONS_MANAGER,
    ARCHITECT,
    COMPONENTS_MANAGER,
    COMPONENTS_BROWSER;

    public static String getStringFormatedRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new NotFoundException("Role [" + role + "] cannot be found");
        }
        String goodRoleToAdd = role.toUpperCase();
        try {
            return Role.valueOf(goodRoleToAdd).toString();
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Role [" + role + "] cannot be found");
        }
    }
}
