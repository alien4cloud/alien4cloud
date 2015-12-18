package alien4cloud.orchestrators.locations.services;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.Usage;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationAutoConfigurer;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.utils.MapUtil;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Manages a locations.
 */
@Slf4j
@Service
public class LocationService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private LocationArchiveIndexer locationArchiveIndexer;
    @Inject
    private LocationResourceService locationResourceService;
    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    /**
     * Auto-configure locations using the given location auto-configurer.
     *
     * @param orchestrator The id of the orchestrator that own the locations.
     * @param locationAutoConfigurer The auto-configurer to use for getting locations.
     */
    public void autoConfigure(Orchestrator orchestrator, ILocationAutoConfigurer locationAutoConfigurer) {
        List<Location> locations = locationAutoConfigurer.getLocations();
        for (Location location : locations) {
            //
            location.setId(UUID.randomUUID().toString());
            location.setOrchestratorId(orchestrator.getId());
            try {
                createLocation(orchestrator, location, location.getInfrastructureType());
            } catch (AlreadyExistException e) {
                log.debug("Location <" + location.getName() + "> is already configured for this location - skipping", e);
            }
        }
    }

    /**
     * Add a new locations for a given orchestrator.
     */
    public String create(String orchestratorId, String locationName, String infrastructureType) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        if (!OrchestratorState.CONNECTED.equals(orchestrator.getState())) {
            // we cannot configure locations for orchestrator that are not connected.
            // TODO throw exception
        }
        Location location = new Location();
        location.setId(UUID.randomUUID().toString());
        location.setName(locationName);
        location.setOrchestratorId(orchestratorId);

        createLocation(orchestrator, location, infrastructureType);

        return location.getId();
    }

    private void createLocation(Orchestrator orchestrator, Location location, String infrastructureType) {
        ensureNameUnicityAndSave(location);

        // TODO checks that the infrastructure type is valid
        location.setInfrastructureType(infrastructureType);

        // TODO add User and Group managed by the Orchestrator security

        Set<CSARDependency> dependencies = locationArchiveIndexer.indexArchives(orchestrator, location);
        location.setDependencies(dependencies);

        // save the new location
        alienDAO.save(location);

        autoConfigure(orchestrator, location);
    }

    /**
     * Trigger plugin auto-configuration for the given location.
     *
     * @param locationId Id of the location.
     */
    public List<LocationResourceTemplate> autoConfigure(String locationId) {
        Location location = getOrFail(locationId);
        Orchestrator orchestrator = orchestratorService.getOrFail(location.getOrchestratorId());

        List<LocationResourceTemplate> generatedLocationResources = autoConfigure(orchestrator, location);

        if (CollectionUtils.isEmpty(generatedLocationResources)) {
            // if the orchestrator doesn't support auto-configuration
            // TODO throw exception or just return false ?
        }

        return generatedLocationResources;
    }

    /**
     * This method calls the orchestrator plugin to try to auto-configure the
     *
     * @param orchestrator The orchestrator for which to auto-configure a location.
     * @param location The location to auto-configure
     * @return the List of {@link LocationResourceTemplate} generated from the location auto-configuration call, null is a valid answer.
     */
    private List<LocationResourceTemplate> autoConfigure(Orchestrator orchestrator, Location location) {
        // get the orchestrator plugin instance
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) orchestratorPluginService.getOrFail(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());

        ILocationResourceAccessor accessor = locationResourceService.accessor(location.getId());

        // let's try to auto-configure the location
        List<LocationResourceTemplate> templates = configuratorPlugin.instances(accessor);

        if (templates != null) {
            // save the instances
            for (LocationResourceTemplate template : templates) {
                // initialize the instances from data.
                template.setId(UUID.randomUUID().toString());
                template.setLocationId(location.getId());
                template.setGenerated(true);
                template.setEnabled(true);
                IndexedNodeType nodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, template.getTemplate().getType(),
                        location.getDependencies());
                nodeType.getDerivedFrom().add(0, template.getTemplate().getType());
                template.setTypes(nodeType.getDerivedFrom());
                // FIXME Workaround to remove default scalable properties from compute
                TopologyUtils.setNullScalingPolicy(template.getTemplate(), nodeType);
            }
            alienDAO.save(templates.toArray(new LocationResourceTemplate[templates.size()]));
            location.setLastUpdateDate(new Date());
            alienDAO.save(location);
        }
        return templates;
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
     * Get multiple locations from list of ids
     *
     * @param ids ids of location to get
     * @return map of id to location
     */
    public Map<String, Location> getMultiple(Collection<String> ids) {
        List<Location> locations = alienDAO.findByIds(Location.class, ids.toArray(new String[ids.size()]));
        Map<String, Location> locationMap = Maps.newHashMap();
        for (Location location : locations) {
            locationMap.put(location.getId(), location);
        }
        return locationMap;
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
     * @return true if the location was successfully , false if not.
     */
    public boolean delete(String id) {
        Map<String, String[]> filters = Maps.newHashMap();
        addFilter(filters, "locationIds", id);
        addFilter(filters, "endDate", "null");
        long count = alienDAO.count(Deployment.class, null, filters);
        if (count > 0) {
            return false;
        }
        Location location = getOrFail(id);

        // delete all archives associated with this location only, if possible of course
        Map<Csar, List<Usage>> usages = locationArchiveIndexer.deleteArchives(location);
        if (MapUtils.isNotEmpty(usages)) {
            // TODO what to do when some archives were not deleted?
            log.warn("Some archives for location were not deletec! \n" + usages);
        }
        // delete all location resources for the given location
        alienDAO.delete(LocationResourceTemplate.class, QueryBuilders.termQuery("locationId", id));
        // delete the location
        alienDAO.delete(Location.class, id);

        return true;
    }

    /**
     * Query for all locations given an orchestrator
     *
     * @param orchestratorId Id of the orchestrators for which to get locations.
     * @return An array that contains all locations for the given orchestrators.
     */
    public Location[] getOrchestratorLocations(String orchestratorId) {
        GetMultipleDataResult<Location> locations = alienDAO.search(Location.class, null,
                MapUtil.newHashMap(new String[] { "orchestratorId" }, new String[][] { new String[] { orchestratorId } }), Integer.MAX_VALUE);
        return locations.getData();
    }

    /**
     * Get all locations managed by all the provided orchestrators ids
     *
     * @param orchestratorIds
     * @return
     */
    public List<Location> getOrchestratorsLocations(Collection<String> orchestratorIds) {
        List<Location> locations = null;
        locations = alienDAO.customFindAll(Location.class, QueryBuilders.termsQuery("orchestratorId", orchestratorIds));
        return locations == null ? Lists.<Location> newArrayList() : locations;
    }

    /**
     * Retrieve location given a list of ids, and a specific context
     * Only retrieves the authorized ones.
     *
     * @param fetchContext The fetch context to recover only the required field (Note that this should be simplified to directly use the given field...).
     * @param ids array of id of the applications to find
     * @return Map of locations that has the given ids and for which the user is authorized (key is application Id), or null if no location matching the
     *         request is found.
     */
    public Map<String, Location> findByIdsIfAuthorized(String fetchContext, String... ids) {
        List<Location> results = alienDAO.findByIdsWithContext(Location.class, fetchContext, ids);
        if (results == null) {
            return null;
        }
        Map<String, Location> locations = Maps.newHashMap();
        Iterator<Location> iterator = results.iterator();
        while (iterator.hasNext()) {
            Location location = iterator.next();
            if (!AuthorizationUtil.hasAuthorizationForLocation(location, DeployerRole.values())) {
                iterator.remove();
                continue;
            }
            locations.put(location.getId(), location);
        }
        return locations.isEmpty() ? null : locations;
    }

    private void addFilter(Map<String, String[]> filters, String property, String... values) {
        filters.put(property, values);
    }

    /**
     * Ensure that the location name is unique on the orchestrator before saving it.
     *
     * @param location The location to save.
     * @param oldName
     */
    public synchronized void ensureNameUnicityAndSave(Location location, String oldName) {
        if (StringUtils.isBlank(oldName) || !Objects.equal(location.getName(), oldName)) {
            // check that a location of this name and managed by the same orchestrator doesn't already exists
            QueryBuilder mustQuery = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("name", location.getName()))
                    .must(QueryBuilders.termQuery("orchestratorId", location.getOrchestratorId()));
            if (alienDAO.count(Location.class, mustQuery) > 0) {
                throw new AlreadyExistException("a location with the given name <" + location.getName() + "> already exists on this orchestrator .");
            }
        }
        alienDAO.save(location);
    }

    private void ensureNameUnicityAndSave(Location location) {
        ensureNameUnicityAndSave(location, null);
    }
}