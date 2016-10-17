package org.alien4cloud.tosca.catalog.index;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.Usage;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.plugin.aop.Overridable;

public interface ICsarService {

    /**
     * Get all archive matching the given set of filters.
     *
     * @param filters The filters to query the archives.
     * @param name The name of the archive.
     * @return Return the matching
     */
    long count(Map<String, String[]> filters, String name);

    /**
     * Get a cloud service archive.
     *
     * @param name The name of the archive.
     * @param version The version of the archive.
     * @return The {@link Csar Cloud Service Archive} if found in the repository or null.
     */
    Csar get(String name, String version);

    /**
     *
     * Get a cloud service archive.
     *
     * @param id The id of the archive to retrieve
     * @return
     */
    Csar get(String id);

    /**
     * @return an array of CSARs that depend on this name:version.
     */
    Csar[] getDependantCsars(String name, String version);

    /**
     * Get teh topologies that depends on this csar.
     * Do not return a topology if this csar is his own
     *
     * @return an array of <code>Topology</code>s that depend on this name:version.
     */
    Topology[] getDependantTopologies(String name, String version);

    List<Csar> getTopologiesCsar(Topology... topologies);

    /**
     * @return an array of CSARs that depend on this name:version.
     */
    Location[] getDependantLocations(String name, String version);

    /**
     * Save a Cloud Service Archive in ElasticSearch.
     *
     * @param csar The csar to save.
     */
    void save(Csar csar);

    /**
     * Set dependencies to an existing CSAR
     *
     * @param csarId id of the CSAR
     * @param dependencies the new dependencies
     */
    void setDependencies(String csarId, Set<CSARDependency> dependencies);

    Map<String, Csar> findByIds(String fetchContext, String... ids);

    /**
     *
     * Get a cloud service archive, or fail if not found
     *
     * @param id The id of the archive to retrieve
     * @return The {@link Csar Cloud Service Archive} if found in the repository
     */
    Csar getOrFail(String id);

    /**
     * Get a cloud service archive, or fail with {@link NotFoundException} if not found
     *
     * @param name The name of the archive.
     * @param version The version of the archive.
     * @return The {@link Csar Cloud Service Archive} if found in the repository.
     */
    Csar getOrFail(String name, String version);

    /**
     * @return true if the CSar is a dependency for another or used in a topology.
     */
    boolean isDependency(String csarName, String csarVersion);

    /**
     * Delete an archive if no topology depends from it.
     *
     * @param csarId The id of the archive to delete.
     */
    void forceDeleteCsar(String csarId);

    /**
     * Delete an archive if no topology depends from it.
     *
     * @param csarId The id of the archive to delete.
     */
    void deleteCsar(String csarId);

    void deleteCsar(Csar csar);

    /**
     * Delete the content of the csar from the repository: elements, topologies
     *
     * @param csar
     */
    void deleteCsarContent(Csar csar);

    /**
     * Delete an archive an all its registered / saved elements
     * Abort the deletion if the archive is used by some resources
     *
     * @param csar
     * @return A List of {@link Usage} representing the resources using this archive.
     */
    List<Usage> deleteCsarWithElements(Csar csar);

    /**
     * Get the list of resources that are using the given archive.
     *
     * @param csar The archive for which to get usage.
     * @return The list of usage of the archive.
     */
    List<Usage> getCsarRelatedResourceList(Csar csar);

    /**
     * Generate resources related to a csar list
     *
     * @param csars
     * @return
     */
    List<Usage> generateCsarsInfo(Csar[] csars);

    /**
     * Generate resources related to a locations list
     *
     * @param locations
     * @return
     */
    List<Usage> generateLocationsInfo(Location[] locations);

    /**
     * Generate resources (application or template) related to a topology list
     *
     * @param topologies
     * @return
     */
    List<Usage> generateTopologiesInfo(Topology[] topologies);

    /**
     * Search for Abstract tosca types in the catalog.
     *
     * @param query The text query.
     * @param from from index.
     * @param size The size of the query.
     * @param filters Filters.
     * @return A faceted search result that provides both result data and facets.
     */
    @Overridable
    FacetedSearchResult search(String query, int from, int size, Map<String, String[]> filters);
}
