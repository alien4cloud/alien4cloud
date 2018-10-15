package alien4cloud.application;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.common.MetaPropertyTarget;
import alien4cloud.model.common.Tag;
import org.alien4cloud.alm.events.AfterApplicationDeleted;
import org.alien4cloud.alm.events.BeforeApplicationDeleted;
import org.alien4cloud.tosca.catalog.index.ArchiveImageLoader;
import org.alien4cloud.tosca.model.templates.Topology;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.elasticsearch.common.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService.DeleteApplicationEnvironments;
import alien4cloud.application.ApplicationVersionService.DeleteApplicationVersions;
import alien4cloud.common.ResourceUpdateInterceptor;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.images.ImageDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.IResourceRoles;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.utils.NameValidationUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to manage applications.
 */
@Slf4j
@Service
public class ApplicationService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEventPublisher publisher;
    @Resource
    private ImageDAO imageDAO;
    @Resource
    private ResourceUpdateInterceptor resourceUpdateInterceptor;
    @Inject
    private MetaPropertiesService metaPropertiesService;

    /**
     * Create a new application and return it's id
     *
     * @param user The user that is creating the application (will be APPLICATION_MANAGER)
     * @param archiveName The unique archive name (and if for the application).
     * @param name The name of the new application.
     * @param description The description of the new application.
     * @return The id of the newly created application.
     */
    public String create(String user, String archiveName, String name, String description, Topology template) {
        checkApplicationId(archiveName);
        checkApplicationName(name);

        Application application = new Application();
        application.setId(archiveName);

        Map<String, Set<String>> userRoles = Maps.newHashMap();
        userRoles.put(user, Sets.newHashSet(ApplicationRole.APPLICATION_MANAGER.toString()));
        application.setUserRoles(userRoles);

        application.setName(name);
        application.setDescription(description);

        application.setTags(Lists.newArrayList());
        application.setMetaProperties(Maps.newHashMap());
        if (template != null) {
            if (template.getMetaProperties() != null) {
                // we must find the right IDs for meta props
                application.getMetaProperties().putAll(adaptMetapropertiesFromTopologyToApplication(template.getMetaProperties()));
            }
            // if an icon is defined for topology, set the same for application
            Tag iconTag = ArchiveImageLoader.getIconTag(template.getTags());
            if (iconTag != null) {
                application.setImageId(iconTag.getValue());
            }
        }

        alienDAO.save(application);

        resourceUpdateInterceptor.runOnNewApplication(application);

        return archiveName;
    }

    /**
     * Given the metaproperties comming from topology, if a metaproperty is found on application with the same name,
     * use the Id of application metaproperties.
     *
     * @param templateMetaProperties
     * @return
     */
    private Map<String, String> adaptMetapropertiesFromTopologyToApplication(Map<String, String> templateMetaProperties) {
        Map<String, MetaPropConfiguration> applicationMps = metaPropertiesService.getMetaPropConfigurationsByName(MetaPropertyTarget.APPLICATION);
        Map<String, MetaPropConfiguration> topologyMps = metaPropertiesService.getMetaPropConfigurationsByName(MetaPropertyTarget.TOPOLOGY);
        Map<String, String> metapropTopologyToMapKeyMap = Maps.newHashMap();
        topologyMps.entrySet().forEach(topologyE -> {
            if (applicationMps.containsKey(topologyE.getKey())) {
                metapropTopologyToMapKeyMap.put(topologyE.getValue().getId(), applicationMps.get(topologyE.getKey()).getId());
            }
        });
        Map<String, String> result = Maps.newHashMap();
        templateMetaProperties.forEach((k, v) -> {
            String appMpId = metapropTopologyToMapKeyMap.get(k);
            if (appMpId != null) {
                result.put(appMpId, v);
            }
        });
        return result;
    }

    private void checkApplicationId(String applicationId) {
        // Check that it matches the required pattern
        NameValidationUtils.validateApplicationId(applicationId);

        // Check that it doesn't already exists
        if (alienDAO.findById(Application.class, applicationId) != null) {
            throw new AlreadyExistException("An application with the given id already exists.");
        }
    }

    private void checkApplicationName(String name) {
        NameValidationUtils.validateApplicationName(name);

        if (alienDAO.buildQuery(Application.class).setFilters(singleKeyFilter("name", name)).count() > 0) {
            log.debug("Application name [ {} ] already exists.", name);
            throw new AlreadyExistException("An application with the given name already exists.");
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
        Application application = checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

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
     */
    public boolean delete(String applicationId) {
        // Removal of a deployed application is not authorized
        if (alienDAO.count(Deployment.class, null, fromKeyValueCouples("sourceId", applicationId, "endDate", null)) > 0) {
            return false;
        }
        // Ensure that application related resources can be removed.
        Application application = getOrFail(applicationId);
        DeleteApplicationVersions deleteApplicationVersions = applicationVersionService.prepareDeleteByApplication(applicationId);
        DeleteApplicationEnvironments deleteApplicationEnvironments = applicationEnvironmentService.prepareDeleteByApplication(applicationId);
        // Delete the application.
        deleteApplicationVersions.delete();
        deleteApplicationEnvironments.delete();
        publisher.publishEvent(new BeforeApplicationDeleted(this, applicationId));
        alienDAO.delete(Application.class, applicationId);
        if (application != null && StringUtils.isNotBlank(application.getImageId())) {
            imageDAO.deleteAll(application.getImageId());
        }
        publisher.publishEvent(new AfterApplicationDeleted(this, applicationId));
        return true;
    }

    /**
     * Check if the connected user has at least one application role on the related application with a fail when applicationId is not valid
     * If no roles mentioned, all {@link ApplicationRole} values will be used (one at least required)
     *
     * @param applicationId
     * @return the related application
     */
    public Application checkAndGetApplication(String applicationId, IResourceRoles... roles) {
        Application application = getOrFail(applicationId);
        roles = ArrayUtils.isEmpty(roles) ? ApplicationRole.values() : roles;
        AuthorizationUtil.checkAuthorizationForApplication(application, roles);
        return application;
    }
}
