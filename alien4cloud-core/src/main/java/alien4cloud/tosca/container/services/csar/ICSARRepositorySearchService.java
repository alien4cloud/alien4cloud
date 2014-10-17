package alien4cloud.tosca.container.services.csar;

import java.util.Collection;

import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.container.model.CSARDependency;

/**
 * Service interface to search elements in CSARs.
 */
public interface ICSARRepositorySearchService {

    /**
     * Check if an element exists in the given dependencies.
     * 
     * @param elementClass The element class.
     * @param elementId The TOSCA element id of the element (without archive version).
     * @param dependencies A list of CSAR in which the element may be defined.
     * @return True if the element is found, false if not.
     */
    boolean isElementExistInDependencies(Class<? extends IndexedToscaElement> elementClass, String elementId, Collection<CSARDependency> dependencies);

    /**
     * Get an element from defined dependencies.
     * 
     * @param elementClass The element class.
     * @param elementId The TOSCA element id of the element (without archive version).
     * @param dependencies A list of CSAR in which the element may be defined.
     * @return The requested element or null if the element is not found.
     */
    <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies);

    /**
     * Get an element from defined dependencies.
     * 
     * @param elementClass The element class.
     * @param elementId The TOSCA element id of the element (without archive version).
     * @param dependencies A list of CSAR in which the element may be defined.
     * @return The requested element. The method must throw an {@link NotFoundException}.
     * @throws NotFoundException in case the element cannot be found.
     */
    <T extends IndexedToscaElement> T getRequiredElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies)
            throws NotFoundException;
}