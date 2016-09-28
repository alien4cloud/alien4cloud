package alien4cloud.component;

import java.util.Map;
import java.util.Set;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.AbstractToscaType;

/**
 * Service interface to search elements in CSARs.
 */
public interface ICSARRepositorySearchService {

    /**
     * Get an archive from it's id.
     *
     * @param archiveName The name of the archive.
     * @param archiveVersion The version of the archive.
     * @return The cloud service archive matching the given id.
     */
    Csar getArchive(String archiveName, String archiveVersion);

    /**
     * Check if an element exists in the given dependencies.
     *
     * @param elementClass The element class.
     * @param elementId The TOSCA element id of the element (without archive version).
     * @param dependencies A list of CSAR in which the element may be defined.
     * @return True if the element is found, false if not.
     */
    boolean isElementExistInDependencies(Class<? extends AbstractToscaType> elementClass, String elementId, Set<CSARDependency> dependencies);

    /**
     * Get an element from defined dependencies.
     *
     * @param elementClass The element class.
     * @param dependencies A list of CSAR in which the element may be defined.
     * @param keyValueFilters List of key1, value1, key2, value2 to add term filters to the query for each dependency.
     * @return The requested element or null if the element is not found.
     */
    <T extends AbstractToscaType> T getElementInDependencies(Class<T> elementClass, Set<CSARDependency> dependencies, String... keyValueFilters);

    /**
     * Get an element from defined dependencies.
     *
     * @param elementClass The element class.
     * @param elementId The TOSCA element id of the element (without archive version).
     * @param dependencies A list of CSAR in which the element may be defined.
     * @return The requested element or null if the element is not found.
     */
    <T extends AbstractToscaType> T getElementInDependencies(Class<T> elementClass, String elementId, Set<CSARDependency> dependencies);

    /**
     * Get an element from defined dependencies.
     *
     * @param elementClass The element class.
     * @param elementId The TOSCA element id of the element (without archive version).
     * @param dependencies A list of CSAR in which the element may be defined.
     * @return The requested element. The method must throw an {@link NotFoundException}.
     * @throws NotFoundException in case the element cannot be found.
     */
    <T extends AbstractToscaType> T getRequiredElementInDependencies(Class<T> elementClass, String elementId, Set<CSARDependency> dependencies)
            throws NotFoundException;
}