package alien4cloud.orchestrators.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationSupport;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.security.ResourceRoleService;
import alien4cloud.utils.MapUtil;

/**
 * Manages orchestrators
 */
@Slf4j
@Service
public class OrchestratorService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private OrchestratorFactoriesRegistry orchestratorFactoriesRegistry;
    @Resource
    private LocationService locationService;
    @Resource
    private ResourceRoleService resourceRoleService;

    /**
     * Creates an orchestrators.
     * 
     * @param name The unique name that refers the orchestrators from user point of view.
     * @param pluginId The id of the plugin used to communicate with the orchestrators.
     * @param pluginBean The bean in the plugin that is indeed managing communication.
     * @return The generated identifier for the orchestrators.
     */
    public synchronized String create(String name, String pluginId, String pluginBean) {
        Orchestrator orchestrator = new Orchestrator();
        // generate an unique id
        orchestrator.setId(UUID.randomUUID().toString());
        orchestrator.setName(name);
        orchestrator.setPluginId(pluginId);
        orchestrator.setPluginBean(pluginBean);
        // by default clouds are disabled as it should be configured before being enabled.
        orchestrator.setState(OrchestratorState.DISABLED);
        orchestrator.setAuthorizedUsers(new ArrayList<String>());
        orchestrator.setAuthorizedGroups(new ArrayList<String>());

        // get default configuration for the orchestrators.
        IOrchestratorPluginFactory orchestratorFactory = orchestratorFactoriesRegistry.getPluginBean(orchestrator.getPluginId(), orchestrator.getPluginBean());
        OrchestratorConfiguration configuration = new OrchestratorConfiguration(orchestrator.getId(), orchestratorFactory.getDefaultConfiguration());

        saveAndEnsureNameUnicity(orchestrator);
        alienDAO.save(configuration);

        return orchestrator.getId();
    }

    /**
     * Update the name of an existing orchestrators.
     * 
     * @param id Unique id of the orchestrators.
     * @param name Name of the orchestrators.
     */
    public void updateName(String id, String name) {
        Orchestrator orchestrator = getOrFail(id);
        orchestrator.setName(name);
        saveAndEnsureNameUnicity(orchestrator);
    }

    /**
     * Save the orchestrators but ensure that the name is unique before saving it.
     * 
     * @param orchestrator The orchestrators to save.
     */
    private synchronized void saveAndEnsureNameUnicity(Orchestrator orchestrator) {
        // check that the cloud doesn't already exists
        if (alienDAO.count(Orchestrator.class, QueryBuilders.termQuery("name", orchestrator.getName())) > 0) {
            throw new AlreadyExistException("a cloud with the given name already exists.");
        }
        alienDAO.save(orchestrator);
    }

    /**
     * Delete an existing orchestrators.
     * 
     * @param id The id of the orchestrators to delete.
     */
    public void delete(String id) {
        // delete all locations for the orchestrators
        Location[] locations = locationService.getOrchestratorLocations(id);
        if (locations != null) {
            for (Location location : locations) {
                locationService.delete(location.getId());
            }
        }
        // delete the orchestrators configuration
        alienDAO.delete(OrchestratorConfiguration.class, id);
        alienDAO.delete(Orchestrator.class, id);
    }

    /**
     * Get the orchestrators matching the given id or throw a NotFoundException
     * 
     * @param id If of the orchestrators that we want to get.
     * @return An instance of the orchestrators.
     */
    public Orchestrator getOrFail(String id) {
        Orchestrator orchestrator = alienDAO.findById(Orchestrator.class, id);
        if (orchestrator == null) {
            throw new NotFoundException("Orchestrator [" + id + "] doesn't exists.");
        }
        return orchestrator;
    }

    /**
     * Get multiple orchestrators.
     *
     * @param query The query to apply to filter orchestrators.
     * @param from The start index of the query.
     * @param size The maximum number of elements to return.
     * @param authorizationFilter authorization filter
     * @return A {@link GetMultipleDataResult} that contains Orchestrator objects.
     */
    public GetMultipleDataResult<Orchestrator> search(String query, OrchestratorState status, int from, int size, FilterBuilder authorizationFilter) {
        Map<String, String[]> filters = null;
        if (status != null) {
            filters = MapUtil.newHashMap(new String[] { "status" }, new String[][] { new String[] { status.toString() } });
        }
        return alienDAO.search(Orchestrator.class, query, filters, authorizationFilter, null, from, size);
    }

    /**
     * Get the location support information for a given orchetrator.
     * 
     * @param orchestratorId The id of the orchestrator for which to get location support information.
     * @return location support information.
     */
    public LocationSupport getLocationSupport(String orchestratorId) {
        Orchestrator orchestrator = getOrFail(orchestratorId);
        IOrchestratorPluginFactory orchestratorFactory = orchestratorFactoriesRegistry.getPluginBean(orchestrator.getPluginId(), orchestrator.getPluginBean());
        return orchestratorFactory.getLocationSupport();
    }

    /**
     * Add a user role on all locations for a given orchestrator
     * 
     * @param orchestratorId
     * @param username
     * @param role
     */
    public void addUserRoleOnAllLocations(String orchestratorId, String username, String role) {
        Orchestrator orchestrator = getOrFail(orchestratorId);
        if (!orchestrator.getAuthorizedUsers().contains(username)) {
            orchestrator.getAuthorizedUsers().add(username);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.addUserRole(location, username, role);
            }
            alienDAO.save(orchestrator);
        }
    }

    /**
     * Remove a user role on all locations for a given orchestrator
     * 
     * @param orchestratorId
     * @param username
     * @param role
     */
    public void removeUserRoleOnAllLocations(String orchestratorId, String username, String role) {
        Orchestrator orchestrator = getOrFail(orchestratorId);
        if (orchestrator.getAuthorizedUsers().contains(username)) {
            orchestrator.getAuthorizedUsers().remove(username);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.removeUserRole(location, username, role);
            }
            alienDAO.save(orchestrator);
        }

    }

    /**
     * Add a group role on all locations for a given orchestrator
     * 
     * @param orchestratorId
     * @param groupId
     * @param role
     */
    public void addGroupRoleOnAllLocations(String orchestratorId, String groupId, String role) {
        Orchestrator orchestrator = getOrFail(orchestratorId);
        if (!orchestrator.getAuthorizedGroups().contains(groupId)) {
            orchestrator.getAuthorizedGroups().add(groupId);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.addGroupRole(location, groupId, role);
            }
            alienDAO.save(orchestrator);
        }

    }

    /**
     * Remove a group role on all locations for a given orchestrator
     * 
     * @param orchestratorId
     * @param groupId
     * @param role
     */
    public void removeGroupRoleOnAllLocations(String orchestratorId, String groupId, String role) {
        Orchestrator orchestrator = getOrFail(orchestratorId);
        if (orchestrator.getAuthorizedGroups().contains(groupId)) {
            orchestrator.getAuthorizedGroups().remove(groupId);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.removeGroupRole(location, groupId, role);
            }
            alienDAO.save(orchestrator);
        }
    }
}