package alien4cloud.orchestrators.services;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.csar.services.CsarService;
import alien4cloud.model.common.Tag;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResourceType;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.PaaSProviderService;
import alien4cloud.tosca.model.ArchiveRoot;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceDefinition;
import alien4cloud.utils.MapUtil;

/**
 * Manages a locations.
 */
@Slf4j
@Service
public class LocationService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private PaaSProviderService paaSProviderService;
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private CsarService csarService;

    /**
     * Add a new locations for a given orchestrator.
     */
    public String create(String orchestratorId, String locationName, String infrastructureType) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        if (!OrchestratorState.CONNECTED.equals(orchestrator.getState())) {
            // we cannot configure locations for orchestrator that are not connected.
            // TODO throw exception
        }
        // checks that the infrastructure type is valid
        Location location = new Location();
        location.setId(UUID.randomUUID().toString());
        location.setName(locationName);
        location.setOrchestratorId(orchestratorId);
        location.setInfrastructureType(infrastructureType);

        indexArchives(orchestrator, location);

        // save the new location
        alienDAO.save(location);

        autoConfigure(orchestrator, location);

        return location.getId();
    }

    /**
     * Ensure that plugin archives are indexed, note that by default the archives visibility is not public.
     * 
     * @param orchestrator the orchestrator for which to index archives.
     * @param location The location of the orchestrator for which to index archives.
     */
    private void indexArchives(Orchestrator orchestrator, Location location) {
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) paaSProviderService.getPaaSProvider(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());

        // ensure that the plugin archives for this location are imported in the
        List<ArchiveRoot> archives = configuratorPlugin.pluginArchives();

        // index archive here if not already indexed
        for (ArchiveRoot archive : archives) {
            Csar csar = csarService.getIfExists(archive.getArchive().getName(), archive.getArchive().getVersion());
            if (csar == null) {
                // index the required archive
                indexArchive(archive, orchestrator, location);
            } else {
                // TODO Link csar and archive elements to the location
                log.debug("Archive {}:{} from plugin {}:{} location {} already exists in the repository and won't be updated.", archive.getArchive().getName(),
                        archive.getArchive().getVersion(), orchestrator.getPluginId(), orchestrator.getPluginBean(), location.getInfrastructureType());
            }
        }
    }

    private void indexArchive(ArchiveRoot archive, Orchestrator orchestrator, Location location) {
        // inject a specific tag to allow components catalog filtering search
        injectWorkSpace(archive.getNodeTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getArtifactTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getCapabilityTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getRelationshipTypes().values(), orchestrator, location);
        // index components from the archives

    }

    private void injectWorkSpace(Collection<? extends IndexedToscaElement> elements, Orchestrator orchestrator, Location location) {
        for (IndexedToscaElement element : elements) {
            element.getTags().add(new Tag("alien-workspace-id", orchestrator.getId() + ":" + location.getId()));
            element.getTags().add(new Tag("alien-workspace-name", orchestrator.getName() + " - " + location.getName()));
        }
    }

    /**
     * 
     * @param orchestratorId Id of the orchestrator.
     * @param locationId Id of the location.
     */
    public void autoConfigure(String orchestratorId, String locationId) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        Location location = getOrFail(locationId);
        if (!autoConfigure(orchestrator, location)) {
            // if the orchestrator doesn't support auto-configuration
            // TODO throw exception
        }
    }

    /**
     * This method calls the orchestrator plugin to try to auto-configure the
     *
     * @param orchestrator The orchestrator for which to auto-configure a location.
     * @param location The location to auto-configure
     * @return true if the orchestrator performed auto-configuration, false if the orchestrator cannot perform auto-configuration.
     */
    private boolean autoConfigure(Orchestrator orchestrator, Location location) {
        // get the orchestrator plugin instance
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) paaSProviderService.getPaaSProvider(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());

        // let's try to auto-configure the location
        List<LocationResourceTemplate> templates = configuratorPlugin.instances();
        if (templates == null) {
            return false;
        }
        // save the instances
        for (LocationResourceTemplate template : templates) {
            // initialize the instances from data.
            template.setId(UUID.randomUUID().toString());
            template.setLocationId(location.getId());
        }
        alienDAO.save(templates.toArray(new LocationResourceTemplate[templates.size()]));
        return true;
    }

    /**
     * Get the list of resources definitions for a given orchestrator.
     *
     * @param orchestratorId Id of the orchestrator.
     * @param locationId Id of the location.
     * @param locationResourceType The (optional, may be null) type of resource to retrieve
     * @return A list of resource definitions for the given location.
     */
    public List<LocationResourceDefinition> getResourceDefinition(String orchestratorId, String locationId, LocationResourceType locationResourceType) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        Location location = getOrFail(locationId);

        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) paaSProviderService.getPaaSProvider(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());

        List<LocationResourceDefinition> locationDefinitions = configuratorPlugin.definitions();
        if (locationResourceType == null) {
            return locationDefinitions;
        }
        List<LocationResourceDefinition> filteredDefinitions = Lists.newArrayList();
        for (LocationResourceDefinition definition : locationDefinitions) {
            if (locationResourceType.equals(definition.getLocationResourceType())) {
                filteredDefinitions.add(definition);
            }
        }

        return filteredDefinitions;
    }

    /**
     * Get the location matching the given id or throw a NotFoundException
     *
     * @param id If of the location that we want to get.
     * @return An instance of the location.
     */
    public Location getOrFail(String id) {
        Location location = alienDAO.findById(Location.class, id);
        if (location == null) {
            throw new NotFoundException("Location [" + id + "] doesn't exists.");
        }
        return location;
    }

    /**
     * Return all locations for a given orchestrator.
     *
     * @param orchestratorId The id of the orchestrator for which to get locations.
     * @return
     */
    public List<Location> getAll(String orchestratorId) {
        List<Location> locations = alienDAO.customFindAll(Location.class, QueryBuilders.termQuery("orchestratorId", orchestratorId));
        if (locations == null) {
            return Lists.newArrayList();
        }
        return locations;
    }

    /**
     * Delete a locations.
     * 
     * @param id id of the locations to delete.
     */
    public void delete(String id) {
        // TODO IMPORTANT ensure that no deployment use the location
        // delete all location resources for the given location
        alienDAO.delete(LocationResourceDefinition.class, QueryBuilders.termQuery("locationId", id));
        // delete the location
        alienDAO.delete(Location.class, id);
    }

    /**
     * Query for all locations
     * 
     * @param orchestratorId Id of the orchestrators for which to get locations.
     * @return An array that contains all locations for the given orchestrators.
     */
    public Location[] getOrchestratorLocations(String orchestratorId) {
        GetMultipleDataResult<Location> locations = alienDAO.search(Location.class, null,
                MapUtil.newHashMap(new String[] { "orchestratorId" }, new String[][] { new String[] { orchestratorId } }), Integer.MAX_VALUE);
        return locations.getData();
    }
}