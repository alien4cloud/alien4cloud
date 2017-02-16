package alien4cloud.security;

import java.util.Set;

/**
 * This is temporary to differentiate with {@link ISecuredResource}.
 * An {@link ISecurityEnabledResource} is a secured resource, only authorized subjects have access to the resource.
 */
public interface ISecurityEnabledResource {

    String USER = "user";

    String GROUP = "group";

    String APPLICATION = "application";

    String ENVIRONMENT = "environment";

    /**
     * Get the Id of the secured resource
     * 
     * @return the Id of the secured resource
     */
    String getId();

    /**
     * For the given subject get all of its permissions
     * 
     * @return the subject's permissions
     */
    Set<Permission> getPermissions(Subject subjectType, String subject);

    /**
     * Add permissions to a subject
     * 
     * @param subjectType the type of the subject
     * @param subject the id of the subject
     * @param permissions list of permissions
     */
    void addPermissions(Subject subjectType, String subject, Set<Permission> permissions);

    /**
     * Revoke permissions of a subject
     *
     * @param subjectType the type of the subject
     * @param subject the id of the subject
     * @param permissions list of permissions
     */
    void removePermissions(Subject subjectType, String subject, Set<Permission> permissions);
}
