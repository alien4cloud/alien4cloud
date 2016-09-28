package alien4cloud.orchestrators.plugin;

import java.util.List;
import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;

/**
 * Allows to access the resources of a defined location.
 */
public interface ILocationResourceAccessor {
    /**
     * Get all configured resources for the current location.
     *
     * @return A list with all configured resources for the location this instance is associated with.
     */
    List<LocationResourceTemplate> getResources();

    /**
     * Get all the resources of the given type for a given location.
     *
     * @param type The type of the location resource to get.
     * @return A list with all configured resources of the given type for the location this instance is associated with.
     */
    List<LocationResourceTemplate> getResources(String type);

    /**
     * Get all tosca elements of a given type for a location
     *
     * @param type The type of the tosca element to get
     * @return the tosca elements found given the provided type, in the related dependencies of the location.
     */
    <T extends AbstractToscaType> T getIndexedToscaElement(String type);

    /**
     * Get the set of this location dependencies
     *
     * @return A Set of the location archives dependencies.
     */
    Set<CSARDependency> getDependencies();
}