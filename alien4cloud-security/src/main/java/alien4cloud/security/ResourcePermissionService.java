package alien4cloud.security;

import java.util.Arrays;
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
     * Add admin permission to the given resource for the given subject.
     *
     * @param resource the resource to secure
     * @param subjects list of subjects
     */
    public void grantPermission(ISecurityEnabledResource resource, Subject subjectType, String... subjects) {
        Arrays.stream(subjects).forEach(subject -> resource.addPermissions(subjectType, subject, Sets.newHashSet(Permission.ADMIN)));
        alienDAO.save(resource);
    }

    /**
     * Revoke admin permission from the given resource from the given subjects.
     * 
     * @param resource the resource to revoke
     * @param subjectType the type of the subject
     * @param subjects the subjects from which the permissions are revoked
     */
    public void revokePermission(ISecurityEnabledResource resource, Subject subjectType, String... subjects) {
        Arrays.stream(subjects).forEach(subject -> resource.removePermissions(subjectType, subject, Sets.newHashSet(Permission.ADMIN)));
        alienDAO.save(resource);
    }

    /**
     * Check if the given subject has admin privilege on the given resource.
     * 
     * @param resource the resource
     * @param subjectType subject's type
     * @param subject the subject's id
     * @return true if the subject has admin privilege, false otherwise
     */
    private boolean hasPermission(ISecurityEnabledResource resource, Subject subjectType, String subject) {
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
}
