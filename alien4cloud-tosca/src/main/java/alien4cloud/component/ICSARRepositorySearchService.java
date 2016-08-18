package alien4cloud.component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedToscaElement;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.Collection;

/**
 * Service interface to search elements in CSARs.
 */
public interface ICSARRepositorySearchService {

    /**
     * Get an archive from it's id.
     *
     * @param id The id of the archive.
     * @return The cloud service archive matching the given id.
     */
    Csar getArchive(String id);

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
     * Get an element matching specified query from given dependencies
     * 
     * @param elementClass The element class.
     * @param query query to match element
     * @param dependencies A list of CSAR in which the element may be defined.
     * @param <T> type of the tosca element
     * @return
     */
    <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, QueryBuilder query, Collection<CSARDependency> dependencies);

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

    /**
     * GEt the parent of an element
     *
     * @param elementClass
     * @param indexedToscaElement
     * @param parentElementId
     * @return
     */
    <T extends IndexedToscaElement> T getParentOfElement(Class<T> elementClass, T indexedToscaElement, String parentElementId);

    // FacetedSearchResult search(Class<? extends IndexedToscaElement> classNameToQuery, String query, Integer from, Integer size, Map<String, String[]>
    // filters,
    // boolean queryAllVersions);

}