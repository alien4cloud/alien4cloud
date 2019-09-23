package alien4cloud.orchestrators.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.ArtifactSupport;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationSupport;
import alien4cloud.orchestrators.events.AfterOrchestratorCreated;
import alien4cloud.orchestrators.events.AfterOrchestratorDeleted;
import alien4cloud.orchestrators.events.BeforeOrchestratorDeleted;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages orchestrators
 */
@Slf4j
@Service
public class OrchestratorService {
    public static final String[] ENABLED_STATES = new String[] { OrchestratorState.CONNECTED.toString(), OrchestratorState.CONNECTING.toString(),
            OrchestratorState.DISCONNECTED.toString() };

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private OrchestratorFactoriesRegistry orchestratorFactoriesRegistry;
    @Inject
    private LocationService locationService;

    @Inject
    private ApplicationEventPublisher publisher;

    /**
     * Creates an orchestrator.
     *
     * @param name The unique name that defines the orchestrator from user point of view.
     * @param pluginId The id of the plugin used to communicate with the orchestrator.
     * @param pluginBean The bean in the plugin that is indeed managing communication.
     * @return The generated identifier for the orchestrator.
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

        // get default configuration for the orchestrator.
        IOrchestratorPluginFactory orchestratorFactory = getPluginFactory(orchestrator);
        OrchestratorConfiguration configuration = new OrchestratorConfiguration(orchestrator.getId(), orchestratorFactory.getDefaultConfiguration());

        ensureNameUnicityAndSave(orchestrator);
        alienDAO.save(configuration);

        publisher.publishEvent(new AfterOrchestratorCreated(this, orchestrator));
        return orchestrator.getId();
    }

    /**
     * Save the orchestrator but ensure that the name is unique before saving it.
     *
     * @param orchestrator The orchestrator to save.
     */
    private synchronized void ensureNameUnicityAndSave(Orchestrator orchestrator) {
        ensureNameUnicityAndSave(orchestrator, null);
    }

    /**
     * Save the orchestrator but ensure that the name is unique before saving it.
     *
     * @param orchestrator The orchestrator to save.
     */
    public synchronized void ensureNameUnicityAndSave(Orchestrator orchestrator, String oldName) {
        if (StringUtils.isBlank(oldName) || !Objects.equals(orchestrator.getName(), oldName)) {
            // check that the orchestrator doesn't already exists
            if (alienDAO.count(Orchestrator.class, QueryBuilders.termQuery("name", orchestrator.getName())) > 0) {
                throw new AlreadyExistException("a cloud with the given name already exists.");
            }
        }
        alienDAO.save(orchestrator);
    }

    /**
     * Delete an existing orchestrator.
     *
     * @param id The id of the orchestrator to delete.
     */
    public void delete(String id) {
        publisher.publishEvent(new BeforeOrchestratorDeleted(this, id));
        // delete all locations for the orchestrator
        Location[] locations = locationService.getOrchestratorLocations(id);
        if (locations != null) {
            for (Location location : locations) {
                locationService.delete(id, location.getId());
            }
        }

        IOrchestratorPluginFactory pluginFactory = getPluginFactory(get(id));
        pluginFactory.delete(id);

        // delete the orchestrator configuration
        alienDAO.delete(OrchestratorConfiguration.class, id);
        alienDAO.delete(Orchestrator.class, id);
        publisher.publishEvent(new AfterOrchestratorDeleted(this, id));
    }

    /**
     * Get the orchestrator matching the given id
     *
     * @param id If of the orchestrator that we want to get.
     * @return An instance of the orchestrator.
     */
    public Orchestrator get(String id) {
        return alienDAO.findById(Orchestrator.class, id);
    }

    /**
     * Get the orchestrator matching the given id or throw a NotFoundException
     *
     * @param id If of the orchestrator that we want to get.
     * @return An instance of the orchestrator.
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
        return alienDAO.search(Orchestrator.class, query, filters, authorizationFilter, null, from, size, "name.lower_case", false);
    }

    /**
     * Get the location support information for a given orchestrator.
     *
     * @param orchestratorId The id of the orchestrator for which to get location support information.
     * @return location support information.
     */
    public LocationSupport getLocationSupport(String orchestratorId) {
        Orchestrator orchestrator = getOrFail(orchestratorId);
        IOrchestratorPluginFactory orchestratorFactory = getPluginFactory(orchestrator);
        return orchestratorFactory.getLocationSupport();
    }

    /**
     * Get the artifact support information for a given orchestrator.
     *
     * @param orchestratorId The id of the orchestrator for which to get location support information.
     * @return artifact support information.
     */
    public ArtifactSupport getArtifactSupport(String orchestratorId) {
        Orchestrator orchestrator = getOrFail(orchestratorId);
        IOrchestratorPluginFactory orchestratorFactory = getPluginFactory(orchestrator);
        return orchestratorFactory.getArtifactSupport();
    }

    /**
     * Get the orchestrator plugin factory for the given orchestrator.
     *
     * @param orchestrator The orchestrator for which to get the orchestrator plugin factory.
     * @return An instance of the orchestrator plugin factory for the given orchestrator.
     */
    public IOrchestratorPluginFactory getPluginFactory(Orchestrator orchestrator) {
        return orchestratorFactoriesRegistry.getPluginBean(orchestrator.getPluginId(), orchestrator.getPluginBean());
    }

    public List<Orchestrator> getAllEnabledOrchestrators() {
        return alienDAO.customFindAll(Orchestrator.class, QueryBuilders.termsQuery("state", ENABLED_STATES));
    }

    public List<Orchestrator> getAll() {
        return alienDAO.customFindAll(Orchestrator.class, null);
    }

}