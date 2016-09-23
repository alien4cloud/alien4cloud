package org.alien4cloud.tosca.catalog.index;

import java.util.Map;

import org.alien4cloud.tosca.model.types.AbstractToscaType;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.plugin.aop.Overridable;

/**
 * Interface for the ToscaTypeSearchService to allow jdk dynamic proxy.
 */
public interface IToscaTypeSearchService extends ICSARRepositorySearchService {

    /**
     * Return true if the archive contains Tosca Types.
     *
     * @param archiveName The name of the archive.
     * @param archiveVersion The version of the archive.
     * @return True if the archive contains types.
     */
    boolean hasTypes(String archiveName, String archiveVersion);

    /**
     * Find an element based on it's type, id and version.
     *
     * @param elementType The element type.
     * @param elementId The element id.
     * @param version The element version (version of the archive that defines the element).
     * @return Return the matching
     */
    <T extends AbstractToscaType> T find(Class<T> elementType, String elementId, String version);

    /**
     * Find the most recent element from a given id.
     *
     * @param elementType The element type.
     * @param elementId The element id.
     * @return Return the matching
     */
    <T extends AbstractToscaType> T findMostRecent(Class<T> elementType, String elementId);

    /**
     * Get all tosca types available in the CSAR with the given name and version
     *
     * @param archiveName archive's name
     * @param archiveVersion archive's version
     * @return all available tosca types inside the CSAR
     */
    AbstractToscaType[] getArchiveTypes(String archiveName, String archiveVersion);

    /**
     * Find an element based on it's type and id.
     *
     * @param elementType The element type.
     * @param elementId The element id.
     * @return Return the matching
     */
    <T extends AbstractToscaType> T[] findAll(Class<T> elementType, String elementId);

    /**
     * Search for Abstract tosca types in the catalog.
     *
     * @param clazz The class to search for.
     * @param query The text query.
     * @param size The size of the query.
     * @param filters Filters.
     * @return A faceted search result that provides both result data and facets.
     */
    @Overridable
    FacetedSearchResult search(Class<? extends org.alien4cloud.tosca.model.types.AbstractToscaType> clazz, String query, Integer size,
            Map<String, String[]> filters);
}