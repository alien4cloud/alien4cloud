package alien4cloud.security;

import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;

@Service
public class ResourcePermissionService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Add permissions to the given resource
     * 
     * @param resource the resource to secure
     * @param subjectType the type of the subject
     * @param subject the subject to which the permissions are granted
     * @param permissions the permissions to give
     */
    public void addPermissions(ISecurityEnabledResource resource, String subjectType, String subject, Set<String> permissions) {
        resource.addPermissions(subjectType, subject, permissions);
        alienDAO.save(resource);
    }

    public void revokeAllPermissions(ISecurityEnabledResource resource, String subjectType, String subject) {
        resource.revokeAllPermissions(subjectType, subject);
    }
}
