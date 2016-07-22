package alien4cloud.application;

import java.util.*;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.QueryHelper;
import org.elasticsearch.mapping.QueryHelper.SearchQueryHelperBuilder;
import org.springframework.stereotype.Service;

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
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Service to manage applications.
 *
 * @author luc boutier
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
     * @param name The name of the new application.
     * @param description The description of the new application.
     * @return The id of the newly created application.
     */
    public String create(String user, String name, String description) {
        ensureNameIsValid(name);
        ensureNameUnicity(name);

        String id = UUID.randomUUID().toString();

        Application application = new Application();
        application.setId(id);

        Map<String, Set<String>> userRoles = Maps.newHashMap();
        userRoles.put(user, Sets.newHashSet(ApplicationRole.APPLICATION_MANAGER.toString()));
        application.setUserRoles(userRoles);

        application.setName(name);
        application.setDescription(description);
        application.setCreationDate(new Date());
        application.setLastUpdateDate(new Date());

        application.setTags(Lists.<Tag> newArrayList());
        application.setMetaProperties(Maps.<String, String> newHashMap());

        alienDAO.save(application);
        return id;
    }

    /**
     * Check the the name of the application is already used.
     *
     * @param name The name of the application.
     * @return true if an application already use this name, false if not.
     */
    public void ensureNameUnicity(String name) {
        if (alienDAO.count(Application.class, QueryBuilders.termQuery("name", name)) > 0) {
            log.debug("Application name <{}> already exists.", name);
            throw new AlreadyExistException("An application with the given name already exists.");
        }
    }

    /**
     * Check the the name of the application is valid.
     *
     * @param name The name of the application.
     * @return throw an error if invalid.
     */
    public void ensureNameIsValid(String name) {
        if (!isValidNodeName(name)) {
            log.debug("Application name <{}> contains forbidden character.", name);
            throw new InvalidApplicationNameException("An application name should not contains slash or backslash.");
        }
    }

    public static boolean isValidNodeName(String name) {
        return Pattern.matches(APPLICATION_NAME_REGEX, name);
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
        String index = alienDAO.getIndexForType(Deployment.class);
        SearchQueryHelperBuilder searchQueryHelperBuilder = queryHelper.buildSearchQuery(index).types(Deployment.class)
                .filters(MapUtil.newHashMap(new String[] { "sourceId", "endDate" }, new String[][] { new String[] { applicationId }, new String[] { null } }))
                .fieldSort("_timestamp", true);

        GetMultipleDataResult<Object> result = alienDAO.search(searchQueryHelperBuilder, 0, 1);
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
