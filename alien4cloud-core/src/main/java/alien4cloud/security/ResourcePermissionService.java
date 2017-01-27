package alien4cloud.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.alien4cloud.alm.events.BeforeApplicationDeleted;
import org.alien4cloud.alm.events.BeforeApplicationEnvironmentDeleted;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.security.event.GroupDeletedEvent;
import alien4cloud.security.event.UserDeletedEvent;
import alien4cloud.utils.TypeScanner;

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

    private interface ResourcePermissionCleaner {
        void cleanPermission(AbstractSecurityEnabledResource resource, String subjectId);
    }

    private void deletePermissions(FilterBuilder appFilter, String ownerId, ResourcePermissionCleaner permissionCleaner)
            throws IOException, ClassNotFoundException {
        int from = 0;
        long totalResult;

        Set<Class<?>> classes = TypeScanner.scanTypes("alien4cloud.model", AbstractSecurityEnabledResource.class);
        Set<String> indices = classes.stream().map(clazz -> alienDAO.getIndexForType(clazz)).collect(Collectors.toSet());
        do {
            GetMultipleDataResult<Object> result = alienDAO.search(indices.toArray(new String[indices.size()]), classes.toArray(new Class<?>[classes.size()]),
                    null, null, appFilter, null, from, 20);
            Arrays.stream(result.getData()).forEach(resource -> permissionCleaner.cleanPermission((AbstractSecurityEnabledResource) resource, ownerId));
            from += result.getData().length;
            totalResult = result.getTotalResults();
        } while (from < totalResult);
    }

    @EventListener
    public void userDeletedEventListener(UserDeletedEvent event) throws IOException, ClassNotFoundException {
        FilterBuilder resourceFilter = FilterBuilders.nestedFilter("userPermissions",
                FilterBuilders.termFilter("userPermissions.key", event.getUser().getUsername()));
        deletePermissions(resourceFilter, event.getUser().getUsername(), ((resource, subjectId) -> revokePermission(resource, Subject.USER, subjectId)));
    }

    @EventListener
    public void groupDeletedEventListener(GroupDeletedEvent event) throws IOException, ClassNotFoundException {
        FilterBuilder resourceFilter = FilterBuilders.nestedFilter("groupPermissions",
                FilterBuilders.termFilter("groupPermissions.key", event.getGroup().getId()));
        deletePermissions(resourceFilter, event.getGroup().getId(), ((resource, subjectId) -> revokePermission(resource, Subject.GROUP, subjectId)));
    }

    @EventListener
    public void applicationDeletedEventListener(BeforeApplicationDeleted event) throws IOException, ClassNotFoundException {
        FilterBuilder resourceFilter = FilterBuilders.nestedFilter("applicationPermissions",
                FilterBuilders.termFilter("applicationPermissions.key", event.getApplicationId()));
        deletePermissions(resourceFilter, event.getApplicationId(), ((resource, subjectId) -> revokePermission(resource, Subject.APPLICATION, subjectId)));
    }

    @EventListener
    public void environmentDeletedEventListener(BeforeApplicationEnvironmentDeleted event) throws IOException, ClassNotFoundException {
        FilterBuilder resourceFilter = FilterBuilders.nestedFilter("environmentPermissions",
                FilterBuilders.termFilter("environmentPermissions.key", event.getApplicationEnvironmentId()));
        deletePermissions(resourceFilter, event.getApplicationEnvironmentId(),
                ((resource, subjectId) -> revokePermission(resource, Subject.ENVIRONMENT, subjectId)));
    }
}
