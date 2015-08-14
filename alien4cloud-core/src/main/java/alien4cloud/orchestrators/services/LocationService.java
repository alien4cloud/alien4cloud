package alien4cloud.orchestrators.services;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResource;
import alien4cloud.utils.MapUtil;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Manages a locations.
 */
@Service
public class LocationService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Add a new locations for a given orchestrators.
     */
    public void create(String orchestratorId) {

    }

    /**
     * Get a specific locations based on it's id.
     * 
     * @param id The id of the locations to get.
     */
    public Location get(String id) {
        return alienDAO.findById(Location.class, id);
    }

    /**
     * Delete a locations.
     * 
     * @param id id of the locations to delete.
     */
    public void delete(String id) {
        // delete all location resources for the given location
        alienDAO.delete(LocationResource.class, QueryBuilders.termQuery("locationId", id));
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