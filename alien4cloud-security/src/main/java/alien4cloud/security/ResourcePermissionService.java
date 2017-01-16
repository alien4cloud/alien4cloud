package alien4cloud.security;

import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;

/**
 * Service managing permissions to resources
 */
@Service
public class ResourcePermissionService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Add admin permission to the given resource.
     * 
     * @param resource the resource to secure
     * @param subjectType the type of the subject
     * @param subject the subject to which the permissions are granted
     */
    public void grantAdminPermission(ISecurityEnabledResource resource, Subject subjectType, String subject) {
        grantPermissions(resource, subjectType, subject, Sets.newHashSet(Permission.ADMIN));
    }

    /**
     * Revoke admin permission of the given resource.
     * 
     * @param resource the resource to revoke
     * @param subjectType the type of the subject
     * @param subject the subject to which the permissions are revoked
     */
    public void revokeAdminPermission(ISecurityEnabledResource resource, Subject subjectType, String subject) {
        revokePermissions(resource, subjectType, subject, Sets.newHashSet(Permission.ADMIN));
    }

    /**
     * Check if the given subject has admin privilege on the given resource.
     * 
     * @param resourceType resource's type
     * @param subjectType subject's type
     * @param subject the subject's id
     * @param <T> type of the secured resource
     * @return true if the subject has admin privilege, false otherwise
     */
    public <T extends ISecurityEnabledResource> boolean hasAdminPermission(String resourceType, Subject subjectType, String subject) {
        ISecurityEnabledResource resource = retrieveSecuredResource(resourceType, subject);
        return resource.getPermissions(subjectType, subject).contains(Permission.ADMIN);
    }

    private <T extends ISecurityEnabledResource> T retrieveSecuredResource(String resourceType, String subject) {
        return alienDAO.findById((Class<T>) alienDAO.getClassFromType(resourceType), subject);
    }

    private void grantPermissions(ISecurityEnabledResource resource, Subject subjectType, String subject, Set<Permission> permissions) {
        resource.addPermissions(subjectType, subject, permissions);
        alienDAO.save(resource);
    }

    private void revokePermissions(ISecurityEnabledResource resource, Subject subjectType, String subject, Set<Permission> permissions) {
        resource.removePermissions(subjectType, subject, permissions);
        alienDAO.save(resource);
    }
}
