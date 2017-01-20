package alien4cloud.security;

import java.util.Map;
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
    public void grantPermission(ISecurityEnabledResource resource, Subject subjectType, String subject) {
        grantPermissions(resource, subjectType, subject, Sets.newHashSet(Permission.ADMIN));
    }

    /**
     * Revoke admin permission of the given resource.
     * 
     * @param resource the resource to revoke
     * @param subjectType the type of the subject
     * @param subject the subject to which the permissions are revoked
     */
    public void revokePermission(ISecurityEnabledResource resource, Subject subjectType, String subject) {
        revokePermissions(resource, subjectType, subject, Sets.newHashSet(Permission.ADMIN));
    }

    /**
     * Check if the given subject has admin privilege on the given resource.
     * 
     * @param resource the resource
     * @param subjectType subject's type
     * @param subject the subject's id
     * @return true if the subject has admin privilege, false otherwise
     */
    public boolean hasPermission(ISecurityEnabledResource resource, Subject subjectType, String subject) {
        return resource.getPermissions(subjectType, subject).contains(Permission.ADMIN);
    }

    /**
     * Check if the given subjects has admin privilege on the given resource.
     * 
     * @param resource the resource
     * @param subjects the subjects' ids
     * @return true if the subjects have admin privilege, false otherwise
     */
    public boolean hasPermission(ISecurityEnabledResource resource, Map<Subject, Set<String>> subjects) {
        return subjects.entrySet().stream()
                .anyMatch(subjectEntry -> subjectEntry.getValue().stream().anyMatch(subject -> hasPermission(resource, subjectEntry.getKey(), subject)));
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
