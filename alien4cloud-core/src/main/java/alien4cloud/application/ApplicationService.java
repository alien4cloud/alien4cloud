package alien4cloud.application;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.elasticsearch.mapping.QueryHelper;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidApplicationNameException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.Tag;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.topology.TopologyUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to manage applications.
 */
@Slf4j
@Service
public class ApplicationService {
    @Resource
    private QueryHelper queryHelper;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private ApplicationVersionService applicationVersionService;

    private static final String APPLICATION_NAME_REGEX = "[^/\\\\\\\\]+";

    /**
     * Create a new application and return it's id
     *
     * @param user The user that is creating the application (will be APPLICATION_MANAGER)
     * @param archiveName The unique archive name (and if for the application).
     * @param name The name of the new application.
     * @param description The description of the new application.
     * @return The id of the newly created application.
     */
    public String create(String user, String archiveName, String name, String description) {
        checkApplicationId(archiveName);
        checkApplicationName(name);

        Application application = new Application();
        application.setId(archiveName);

        Map<String, Set<String>> userRoles = Maps.newHashMap();
        userRoles.put(user, Sets.newHashSet(ApplicationRole.APPLICATION_MANAGER.toString()));
        application.setUserRoles(userRoles);

        application.setName(name);
        application.setDescription(description);

        application.setTags(Lists.<Tag> newArrayList());
        application.setMetaProperties(Maps.<String, String> newHashMap());

        alienDAO.save(application);
        return archiveName;
    }

    private void checkApplicationId(String applicationId) {
        // Check that it matches the required pattern
        if (!TopologyUtils.isValidNodeName(applicationId)) {
            // FIXME throw another exception ?
            throw new InvalidApplicationNameException("Application id <" + applicationId + "> is not valid. It must not contains any special characters.");
        }
        // Check that it doesn't already exists
        if (alienDAO.findById(Application.class, applicationId) != null) {
            throw new AlreadyExistException("An application with the given id already exists.");
        }
    }

    private void checkApplicationName(String name) {
        if (alienDAO.buildQuery(Application.class).setFilters(singleKeyFilter("name", name)).count() > 0) {
            log.debug("Application name <{}> already exists.", name);
            throw new AlreadyExistException("An application with the given name already exists.");
        }
        if (!Pattern.matches(APPLICATION_NAME_REGEX, name)) {
            log.debug("Application name <{}> contains forbidden character.", name);
            throw new InvalidApplicationNameException("Application name <" + name + "> contains forbidden character.");
        }
    }

    /**
     * Update the name and description of an application.
     * 
     * @param applicationId The application id.
     * @param newName The new name for the application.
     * @param newDescription The new description for the application.
     */
    public void update(String applicationId, String newName, String newDescription) {
        Application application = getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        if (newName != null && !newName.isEmpty() && !application.getName().equals(newName)) {
            checkApplicationName(newName);
            application.setName(newName);
        }
        if (newDescription != null) {
            application.setDescription(newDescription);
        }

        alienDAO.save(application);
    }

    /**
     * Get an application from it's id and throw a {@link NotFoundException} in case no application matches the requested id.
     *
     * @param applicationId The id of the application to retrieve.
     * @return The requested application.
     */
    public Application getOrFail(String applicationId) {
        Application application = alienDAO.findById(Application.class, applicationId);
        if (application == null) {
            throw new NotFoundException("Application [" + applicationId + "] cannot be found");
        }
        return application;
    }

    /**
     * Retrieve applications given a list of ids, and a specific context
     * Only retrieves the authorized ones.
     *
     * @param fetchContext The fetch context to recover only the required field (Note that this should be simplified to directly use the given field...).
     * @param ids array of id of the applications to find
     * @return Map of Applications that has the given ids and for which the user is authorized (key is application Id), or null if no application matching the
     *         request is found.
     */
    public Map<String, Application> findByIdsIfAuthorized(String fetchContext, String... ids) {
        List<Application> apps = alienDAO.findByIdsWithContext(Application.class, fetchContext, ids);
        if (apps == null) {
            return null;
        }
        Map<String, Application> applications = Maps.newHashMap();
        Iterator<Application> iterator = apps.iterator();
        while (iterator.hasNext()) {
            Application app = iterator.next();
            if (!AuthorizationUtil.hasAuthorizationForApplication(app, ApplicationRole.values())) {
                iterator.remove();
                continue;
            }
            applications.put(app.getId(), app);
        }
        return applications.isEmpty() ? null : applications;
    }

    /**
     * Delete an existing application from it's id. This method ensures first that there is no running deployment of the application.
     *
     * @param applicationId The id of the application to remove.
     * @return True if the application has been removed, false if not.
     * @throws alien4cloud.paas.exception.OrchestratorDisabledException
     */
    public boolean delete(String applicationId) throws OrchestratorDisabledException {
        // ensure that there is no active deployment(s).
        GetMultipleDataResult<Deployment> result = alienDAO.buildQuery(Deployment.class)
                .setFilters(fromKeyValueCouples("sourceId", applicationId, "endDate", null)).prepareSearch().search(0, 1);

        if (result.getData().length > 0) {
            return false;
        }

        // delete the application
        applicationVersionService.deleteByApplication(applicationId);
        applicationEnvironmentService.deleteByApplication(applicationId);
        alienDAO.delete(Application.class, applicationId);
        return true;
    }

    /**
     * Check if the connected user has at least one application role on the related application with a fail when applicationId is not valid
     * If no roles mentioned, all {@link ApplicationRole} values will be used (one at least required)
     *
     * @param applicationId
     * @return the related application
     */
    public Application checkAndGetApplication(String applicationId, ApplicationRole... roles) {
        Application application = getOrFail(applicationId);
        roles = (roles == null || roles.length == 0) ? ApplicationRole.values() : roles;
        AuthorizationUtil.checkAuthorizationForApplication(application, roles);
        return application;
    }
}
