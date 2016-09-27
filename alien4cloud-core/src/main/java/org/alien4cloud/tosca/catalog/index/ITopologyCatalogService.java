package org.alien4cloud.tosca.catalog.index;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.plugin.aop.Overridable;
import org.alien4cloud.tosca.model.templates.Topology;

import java.util.Map;

/**
 * Interface for the TopologyCatalogService to allow jdk dynamic proxy.
 */
public interface ITopologyCatalogService {
    /**
     * Creates a topology and register it as a template in the catalog
     *
     * @param name The name of the topology template
     * @param description The description of the topology template
     * @param version The version of the topology
     * @param workspace The workspace in which to create the topology.
     * @param fromTopologyId The id of an existing topology to use to create the new topology.
     * @return The @{@link Topology} newly created
     */
    Topology createTopologyAsTemplate(String name, String description, String version, String workspace, String fromTopologyId);

    /**
     * Get all topologies matching the given set of filters.
     *
     * @param filters The filters to query the topologies.
     * @param archiveName The name of the related archive
     * @return Return the matching
     */
    Topology[] getAll(Map<String, String[]> filters, String archiveName);

    /**
     * Get a single topology from it's id or throw a NotFoundException.
     *
     * @param id The id of the topology to look for.
     * @return The topology matching the requested id.
     */
    Topology getOrFail(String id);

    /**
     * Get a single topology from it's id or null if it does not exists.
     *
     * @param id The id of the topology to look for.
     * @return The topology matching the requested id or null if not found.
     */
    Topology get(String id);

    /**
     * Return true if the given id exists.
     *
     * @param id The id to check.
     * @return True if a topology with the given id exists, false if not.
     */
    boolean exists(String id);

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
    FacetedSearchResult search(Class<? extends Topology> clazz, String query, Integer size, Map<String, String[]> filters);
}
