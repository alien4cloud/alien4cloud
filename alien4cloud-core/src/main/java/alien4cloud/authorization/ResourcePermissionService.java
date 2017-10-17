package alien4cloud.authorization;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.security.AbstractSecurityEnabledResource;
import alien4cloud.security.ISecurityEnabledResource;
import alien4cloud.security.Permission;
import alien4cloud.security.Subject;
import alien4cloud.security.groups.IAlienGroupDao;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.User;
import alien4cloud.security.users.IAlienUserDao;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.alien4cloud.alm.events.AfterPermissionRevokedEvent;
import org.alien4cloud.alm.events.BeforePermissionRevokedEvent;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.common.collect.Lists;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service managing permissions to resources
 */
@Service
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class ResourcePermissionService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Inject
    private IAlienUserDao alienUserDao;

    @Inject
    private IAlienGroupDao alienGroupDao;

    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;

    @Inject
    private ApplicationEventPublisher publisher;

    /**
     * Add admin permission to the given resource for the given subject.
     *
     * @param resource the resource to secure
     * @param subjects list of subjects
     */
    public void grantPermission(ISecurityEnabledResource resource, Subject subjectType, String... subjects) {
        grantPermission(resource, (resource1 -> alienDAO.save(resource1)), subjectType, subjects);
    }

    /**
     * Add admin permission to the given resource for the given subject.
     *
     * @param resource the resource to secure
     * @param saver a callback to save the resource after modification
     * @param subjects list of subjects
     */
    public void grantPermission(ISecurityEnabledResource resource, IResourceSaver saver, Subject subjectType, String... subjects) {
        Arrays.stream(subjects).forEach(subject -> resource.addPermissions(subjectType, subject, Sets.newHashSet(Permission.ADMIN)));
        if (saver != null) {
            saver.save(resource);
        }
    }

    /**
     * Revoke admin permission from the given resource from the given subjects.
     *
     * @param resource the resource to revoke
     * @param subjectType the type of the subject
     * @param subjects the subjects from which the permissions are revoked
     */
    public void revokePermission(ISecurityEnabledResource resource, Subject subjectType, String... subjects) {
        revokePermission(resource, (resource1 -> alienDAO.save(resource1)), subjectType, subjects);
    }

    /**
     * Revoke admin permission from the given resource from the given subjects.
     *
     * @param resource the resource to revoke
     * @param saver a callback to save the resource after modification
     * @param subjectType the type of the subject
     * @param subjects the subjects from which the permissions are revoked
     */
    public void revokePermission(ISecurityEnabledResource resource, IResourceSaver saver, Subject subjectType, String... subjects) {
        publisher.publishEvent(new BeforePermissionRevokedEvent(this, new BeforePermissionRevokedEvent.OnResource(resource.getClass(), resource.getId()),
                subjectType, subjects));

        Arrays.stream(subjects).forEach(subject -> resource.removePermissions(subjectType, subject, Sets.newHashSet(Permission.ADMIN)));
        if (saver != null) {
            saver.save(resource);
        }

        publisher.publishEvent(new AfterPermissionRevokedEvent(this, new BeforePermissionRevokedEvent.OnResource(resource.getClass(), resource.getId()),
                subjectType, subjects));
    }

    /**
     * Check if the given subject has admin privilege on the given resource.
     *
     * @param resource the resource
     * @param subjectType subject's type
     * @param subject the subject's id
     *
     * @return true if the subject has admin privilege, false otherwise
     */
    private boolean hasPermission(ISecurityEnabledResource resource, Subject subjectType, String subject) {
        return resource.getPermissions(subjectType, subject).contains(Permission.ADMIN);
    }

    /**
     * Checks if any of the given subjects has admin privilege on the given resource.
     *
     * @param resource the resource
     * @param subjects the subjects' ids
     * @return true if any of the subjects has admin privilege, false otherwise
     */
    public boolean anyHasPermission(ISecurityEnabledResource resource, Map<Subject, Set<String>> subjects) {
        return subjects.entrySet().stream()
                .anyMatch(subjectEntry -> subjectEntry.getValue().stream().anyMatch(subject -> hasPermission(resource, subjectEntry.getKey(), subject)));
    }

    /**
     * Checks if all the given subjects have admin privilege on the given resource.
     *
     * @param resource the resource
     * @param subjects the subjects' ids
     * @return true if all the subjects have admin privilege, false otherwise
     */
    public boolean allHavePermission(ISecurityEnabledResource resource, Map<Subject, Set<String>> subjects) {
        return subjects.entrySet().stream()
                .allMatch(subjectEntry -> subjectEntry.getValue().stream().allMatch(subject -> hasPermission(resource, subjectEntry.getKey(), subject)));
    }

    /**
     * Get summary infos of all authorized users of the resource
     *
     * @param resource
     * @return
     */
    // TODO consider enabling pagination here
    public List<User> getAuthorizedUsers(AbstractSecurityEnabledResource resource) {
        List<User> userDTOs = Lists.newArrayList();
        if (MapUtils.isNotEmpty(resource.getUserPermissions())) {
            List<User> users = alienUserDao.find(resource.getUserPermissions().keySet().toArray(new String[resource.getUserPermissions().size()]));
            users.sort(Comparator.comparing(User::getUsername));
            userDTOs.addAll(users);
        }
        return userDTOs;
    }

    /**
     * Get summary infos of all authorized groups of the resource
     *
     * @param resource
     * @return
     */
    public List<Group> getAuthorizedGroups(AbstractSecurityEnabledResource resource) {
        List<Group> groupDTOS = Lists.newArrayList();
        if (resource.getGroupPermissions() != null && resource.getGroupPermissions().size() > 0) {
            List<Group> groups = alienGroupDao.find(resource.getGroupPermissions().keySet().toArray(new String[resource.getGroupPermissions().size()]));
            groups.sort(Comparator.comparing(Group::getName));
            groupDTOS.addAll(groups);
        }
        return groupDTOS;
    }

    public interface IResourceSaver {
        void save(ISecurityEnabledResource resource);
    }

    public void revokeAuthorizedEnvironmentsAndEnvironmentTypesPerApplication(AbstractSecurityEnabledResource resource, String[] applicationsToDelete,
                                                                              String[] environmentsToDelete, String[] environmentTypesToDelete) {
        IResourceSaver noSave = null;

        if (ArrayUtils.isNotEmpty(applicationsToDelete)) {
            revokePermission(resource, noSave, Subject.APPLICATION, applicationsToDelete);
        }
        if (ArrayUtils.isNotEmpty(environmentsToDelete)) {
            revokePermission(resource, noSave, Subject.ENVIRONMENT, environmentsToDelete);
        }
        if (ArrayUtils.isNotEmpty(environmentTypesToDelete)) {
            revokePermission(resource, noSave, Subject.ENVIRONMENT_TYPE, environmentTypesToDelete);
        }

        alienDAO.save(resource);
    }

    public void grantAuthorizedEnvironmentsAndEnvTypesPerApplication(AbstractSecurityEnabledResource resource, String[] applicationsToAdd, String[] environmentsToAdd,
                                                                     String[] environmentTypesToAdd) {
        List<String> envIds = Lists.newArrayList();
        IResourceSaver noSave = null;

        if (ArrayUtils.isNotEmpty(applicationsToAdd)) {
            grantPermission(resource, noSave, Subject.APPLICATION, applicationsToAdd);
            // when an app is added, all eventual existing env authorizations are removed
            for (String applicationToAddId : applicationsToAdd) {
                ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationToAddId);
                for (ApplicationEnvironment ae : aes) {
                    envIds.add(ae.getId());
                }
            }
            if (!envIds.isEmpty()) {
                revokePermission(resource, noSave, Subject.ENVIRONMENT, envIds.toArray(new String[envIds.size()]));
            }
            // remove all all eventual existing env type authorizations
            Set<String> envTypes = Sets.newHashSet();
            for (String applicationToAddId : applicationsToAdd) {
                for (EnvironmentType envType : EnvironmentType.values()) {
                    envTypes.add(applicationToAddId + ":" + envType.toString());
                }
            }
            if (!envTypes.isEmpty()) {
                revokePermission(resource, noSave, Subject.ENVIRONMENT_TYPE, envTypes.toArray(new String[envTypes.size()]));
            }
        }
        if (ArrayUtils.isNotEmpty(environmentsToAdd)) {
            List<String> envToAddSet = Arrays.stream(environmentsToAdd).filter(env -> !envIds.contains(env)).collect(Collectors.toList());
            grantPermission(resource, noSave, Subject.ENVIRONMENT, envToAddSet.toArray(new String[envToAddSet.size()]));
        }
        if (ArrayUtils.isNotEmpty(environmentTypesToAdd)) {
            grantPermission(resource, noSave, Subject.ENVIRONMENT_TYPE, environmentTypesToAdd);
        }
        alienDAO.save(resource);
    }
}
