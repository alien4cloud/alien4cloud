package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.catalog.repository.CsarFileRepository;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationService;
import alien4cloud.common.AlienConstants;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.Usage;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages cloud services archives and their dependencies.
 */
@Component
@Slf4j
public class CsarService implements ICsarDependencyLoader {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO csarDAO;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Inject
    private IToscaTypeSearchService searchService;
    @Inject
    private IToscaTypeIndexerService indexerService;
    @Inject
    private CsarFileRepository alienRepository;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ITopologyCatalogService catalogService;

    /**
     * Get all archive matching the given set of filters.
     *
     * @param filters The filters to query the archives.
     * @param name The name of the archive.
     * @return Return the matching
     */
    public long count(Map<String, String[]> filters, String name) {
        return csarDAO.buildQuery(Csar.class).setFilters(fromKeyValueCouples(filters, "workspace", AlienConstants.GLOBAL_WORKSPACE_ID, "name", name)).count();
    }

    /**
     * Get all archive matching the given set of filters.
     *
     * @param filters The filters to query the archives.
     * @param name The name of the archive.
     * @return Return the matching
     */
    public Csar[] getAll(Map<String, String[]> filters, String name) {
        return csarDAO.buildQuery(Csar.class).setFilters(fromKeyValueCouples(filters, "workspace", AlienConstants.GLOBAL_WORKSPACE_ID, "name", name))
                .prepareSearch().search(0, Integer.MAX_VALUE).getData();
    }

    /**
     * Get a cloud service archive.
     *
     * @param name The name of the archive.
     * @param version The version of the archive.
     * @return The {@link Csar Cloud Service Archive} if found in the repository or null.
     */
    public Csar get(String name, String version) {
        return csarDAO.buildQuery(Csar.class).setFilters(fromKeyValueCouples("name", name, "version", version)).prepareSearch().find();
    }

    /**
     *
     * Get a cloud service archive.
     *
     * @param id The id of the archive to retrieve
     * @return
     */
    public Csar get(String id) {
        return csarDAO.findById(Csar.class, id);
    }

    @Override
    public Set<CSARDependency> getDependencies(String name, String version) {
        Csar csar = get(name, version);
        if (csar == null) {
            throw new NotFoundException("Csar with name [" + name + "] and version [" + version + "] cannot be found");
        }
        if (csar.getDependencies() == null || csar.getDependencies().isEmpty()) {
            return Sets.newHashSet();
        }
        return Sets.newHashSet(csar.getDependencies());
    }

    /**
     * @return an array of CSARs that depend on this name:version.
     */
    public Csar[] getDependantCsars(String name, String version) {
        FilterBuilder filter = FilterBuilders.nestedFilter("dependencies", FilterBuilders.boolFilter()
                .must(FilterBuilders.termFilter("dependencies.name", name)).must(FilterBuilders.termFilter("dependencies.version", version)));
        GetMultipleDataResult<Csar> result = csarDAO.search(Csar.class, null, null, filter, null, 0, Integer.MAX_VALUE);
        return result.getData();
    }

    /**
     * Get teh topologies that depends on this csar.
     * Do not return a topology if this csar is his own
     * 
     * @return an array of <code>Topology</code>s that depend on this name:version.
     */
    public Topology[] getDependantTopologies(String name, String version) {
        FilterBuilder filter = FilterBuilders.boolFilter()
                .mustNot(FilterBuilders.boolFilter().must(FilterBuilders.termFilter("archiveName", name))
                        .must(FilterBuilders.termFilter("archiveVersion", version)))
                .must(FilterBuilders.nestedFilter("dependencies", FilterBuilders.boolFilter().must(FilterBuilders.termFilter("dependencies.name", name))
                        .must(FilterBuilders.termFilter("dependencies.version", version))));
        GetMultipleDataResult<Topology> result = csarDAO.search(Topology.class, null, null, filter, null, 0, Integer.MAX_VALUE);
        return result.getData();
    }

    public List<Csar> getTopologiesCsar(Topology... topologies) {
        Set<String> ids = Sets.newHashSet();
        for (Topology topology : topologies) {
            ids.add(topology.getId());
        }
        return csarDAO.findByIds(Csar.class, ids.toArray(new String[ids.size()]));
    }

    /**
     * @return an array of CSARs that depend on this name:version.
     */
    public Location[] getDependantLocations(String name, String version) {
        FilterBuilder filter = FilterBuilders.nestedFilter("dependencies", FilterBuilders.boolFilter()
                .must(FilterBuilders.termFilter("dependencies.name", name)).must(FilterBuilders.termFilter("dependencies.version", version)));
        GetMultipleDataResult<Location> result = csarDAO.search(Location.class, null, null, filter, null, 0, Integer.MAX_VALUE);
        return result.getData();
    }

    /**
     * Save a Cloud Service Archive in ElasticSearch.
     *
     * @param csar The csar to save.
     */
    public void save(Csar csar) {
        // save the csar import date
        csar.setImportDate(new Date());
        // fill in transitive dependencies.
        Set<CSARDependency> mergedDependencies = null;
        if (csar.getDependencies() != null) {
            mergedDependencies = Sets.newHashSet(csar.getDependencies());
            for (CSARDependency dependency : csar.getDependencies()) {
                Csar dependencyCsar = get(dependency.getName(), dependency.getVersion());
                if (dependencyCsar != null && dependencyCsar.getDependencies() != null) {
                    // FIXME rebuild the dependency bean, as it (the hash) might ave changed since the archive was first imported
                    // use this.buildDependencyBean instead
                    mergedDependencies.addAll(dependencyCsar.getDependencies());
                }
            }
        }
        csar.setDependencies(mergedDependencies, true);

        this.csarDAO.save(csar);
    }

    /**
     * Set dependencies to an existing CSAR
     * 
     * @param csarId id of the CSAR
     * @param dependencies the new dependencies
     */
    public void setDependencies(String csarId, Set<CSARDependency> dependencies) {
        Csar csar = getOrFail(csarId);
        csar.setDependencies(dependencies);
        save(csar);
    }

    public Map<String, Csar> findByIds(String fetchContext, String... ids) {
        Map<String, Csar> csarMap = Maps.newHashMap();
        List<Csar> csars = csarDAO.findByIdsWithContext(Csar.class, fetchContext, ids);
        for (Csar csar : csars) {
            csarMap.put(csar.getId(), csar);
        }
        return csarMap;
    }

    /**
     *
     * Get a cloud service archive, or fail if not found
     *
     * @param id The id of the archive to retrieve
     * @return The {@link Csar Cloud Service Archive} if found in the repository
     */
    public Csar getOrFail(String id) {
        Csar csar = get(id);
        if (csar == null) {
            throw new NotFoundException("Csar with id [" + id + "] do not exist");
        }
        return csar;
    }

    /**
     * Get a cloud service archive, or fail with {@link NotFoundException} if not found
     *
     * @param name The name of the archive.
     * @param version The version of the archive.
     * @return The {@link Csar Cloud Service Archive} if found in the repository.
     */
    public Csar getOrFail(String name, String version) {
        Csar csar = searchService.getArchive(name, version);
        if (csar == null) {
            throw new NotFoundException("Csar with name [" + name + "] and version [" + version + "] do not exist");
        }
        return csar;
    }

    /**
     * @return true if the CSar is a dependency for another or used in a topology.
     */
    public boolean isDependency(String csarName, String csarVersion) {
        // a csar that is a dependency of another csar
        Csar[] result = getDependantCsars(csarName, csarVersion);
        if (result != null && result.length > 0) {
            return true;
        }
        // check if some of the nodes are used in topologies.
        Topology[] topologies = getDependantTopologies(csarName, csarVersion);
        if (topologies != null && topologies.length > 0) {
            return true;
        }
        return false;
    }

    /**
     * Delete an archive if no topology depends from it.
     *
     * @param csarId The id of the archive to delete.
     */
    public void forceDeleteCsar(String csarId) {
        Csar csar = getOrFail(csarId);
        deleteCsar(csar);
    }

    /**
     * Delete an archive if no topology depends from it.
     *
     * @param csarId The id of the archive to delete.
     */
    public void deleteCsar(String csarId) {
        Csar csar = getOrFail(csarId);
        // a csar that is a dependency of another csar can not be deleted
        if (isDependency(csar.getName(), csar.getVersion())) {
            throw new DeleteReferencedObjectException("This csar can not be deleted since it's a dependencie for others");
        }

        deleteCsar(csar);
    }

    private void deleteCsar(Csar csar) {
        deleteCsarContent(csar);
        csarDAO.delete(Csar.class, csar.getId());
        // physically delete files
        alienRepository.removeCSAR(csar.getName(), csar.getVersion());
    }

    /**
     * Delete the content of the csar from the repository: elements, topologies
     *
     * @param csar
     */
    public void deleteCsarContent(Csar csar) {
        // Delete the topology defined in this archive.
        csarDAO.delete(Topology.class, csar.getId());
        // latest version indicator will be recomputed to match this new reality
        indexerService.deleteElements(csar.getName(), csar.getVersion());
    }

    /**
     * Delete an archive an all its registered / saved elements
     * Abort the deletion if the archive is used by some resources
     *
     * @param csar
     * @return A List of {@link Usage} representing the resources using this archive.
     */
    public List<Usage> deleteCsarWithElements(Csar csar) {
        List<Usage> relatedResourceList = getCsarRelatedResourceList(csar);
        if (relatedResourceList.isEmpty()) {
            deleteCsar(csar);
        }
        return relatedResourceList;
    }

    public Csar getTopologySubstitutionCsar(String topologyId) {
        Csar csarResult = csarDAO.customFind(Csar.class, QueryBuilders.termQuery("substitutionTopologyId", topologyId));
        if (csarResult != null) {
            return csarResult;
        } else {
            return null;
        }
    }

    /**
     * Get the list of resources that are using the given archive.
     * 
     * @param csar The archive for which to get usage.
     * @return The list of usage of the archive.
     */
    public List<Usage> getCsarRelatedResourceList(Csar csar) {
        List<Usage> relatedResourceList = Lists.newArrayList();

        if (csar == null) {
            log.warn("You have requested a resource list for a invalid csar object : <" + csar + ">");
            return relatedResourceList;
        }

        // TODO improve usage infos to add the version of csar/application that uses the given archive.

        // a csar that is a dependency of another csar can not be deleted
        // FIXME WORKSPACE HANDLING REQUIRED
        Csar[] relatedCsars = getDependantCsars(csar.getName(), csar.getVersion());
        if (relatedCsars != null && relatedCsars.length > 0) {
            relatedResourceList.addAll(generateCsarsInfo(relatedCsars));
        }

        // check if some of the nodes are used in topologies.
        Topology[] topologies = getDependantTopologies(csar.getName(), csar.getVersion());
        if (topologies != null && topologies.length > 0) {
            relatedResourceList.addAll(generateTopologiesInfo(topologies));
        }

        // a csar that is a dependency of location can not be deleted
        Location[] relatedLocations = getDependantLocations(csar.getName(), csar.getVersion());
        if (relatedLocations != null && relatedLocations.length > 0) {
            relatedResourceList.addAll(generateLocationsInfo(relatedLocations));
        }

        return relatedResourceList;
    }

    /**
     * Generate resources related to a csar list
     *
     * @param csars
     * @return
     */
    private List<Usage> generateCsarsInfo(Csar[] csars) {
        String resourceName;
        String resourceId;
        List<Usage> resourceList = Lists.newArrayList();
        for (Csar csar : csars) {
            if (ArchiveDelegateType.APPLICATION.toString().equals(csar.getDelegateType())) {
                Application application = applicationService.checkAndGetApplication(csar.getDelegateId());
                resourceName = application.getName();
                resourceId = csar.getDelegateId();
            } else {
                resourceName = csar.getName();
                resourceId = csar.getId();
            }
            Usage temp = new Usage(resourceName, Csar.class.getSimpleName().toLowerCase(), resourceId, csar.getWorkspace());
            resourceList.add(temp);
        }
        return resourceList;
    }

    /**
     * Generate resources related to a locations list
     *
     * @param locations
     * @return
     */
    private List<Usage> generateLocationsInfo(Location[] locations) {
        String resourceName;
        String resourceId;
        List<Usage> resourceList = Lists.newArrayList();
        for (Location location : locations) {
            resourceName = location.getName();
            resourceId = location.getId();
            Usage temp = new Usage(resourceName, Location.class.getSimpleName().toLowerCase(), resourceId, AlienConstants.GLOBAL_WORKSPACE_ID);
            resourceList.add(temp);
        }
        return resourceList;
    }

    /**
     * Checks if a user has the proper rights to delete an archive, based on what it contents
     * 
     * @param csar The archive to delete
     */
    public void checkDeletionAuthorizations(Csar csar) {

        // if the csar is bound to an application, then do not alow the process
        if (Objects.equals(csar.getDelegateType(), ArchiveDelegateType.APPLICATION.toString())) {
            throw new UnsupportedOperationException("Cannot delete an application csar from here ");
        }

        // if this csar has node types, check the COMPONENTS_MANAGER Role
        if (searchService.hasTypes(csar.getName(), csar.getVersion())) {
            AuthorizationUtil.checkHasOneRoleIn(Role.COMPONENTS_MANAGER);
        }

        // if the csar is bound to a topology, check the ARCHITECT Role
        if (catalogService.exists(csar.getId())) {
            AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        }

    }

    /**
     * Generate resources (application or template) related to a topology list
     *
     * @param topologies
     * @return
     */
    private List<Usage> generateTopologiesInfo(Topology[] topologies) {

        List<Usage> resourceList = Lists.newArrayList();

        List<Csar> topologiesCsar = getTopologiesCsar(topologies);
        for (Csar csar : topologiesCsar) {
            if (Objects.equals(csar.getDelegateType(), ArchiveDelegateType.APPLICATION.toString())) {
                // get the related application
                Application application = applicationService.checkAndGetApplication(csar.getDelegateId());
                resourceList.add(new Usage(application.getName(), csar.getDelegateType(), csar.getDelegateId(), csar.getWorkspace()));
                continue;
            } else {
                resourceList.add(new Usage(csar.getName() + "[" + csar.getVersion() + "]", "topologyTemplate", csar.getId(), csar.getWorkspace()));
            }
        }
        return resourceList;
    }

    /**
     * Build a {@link CSARDependency} bean given an archive name and version. This will also fill in the dependency hash.
     *
     * @param name The name of the dependendy
     * @param version The version of the dependency
     * @return
     */
    @ToscaContextual
    public static CSARDependency buildDependencyBean(String name, String version) {
        CSARDependency newDependency = new CSARDependency(name, version);
        Csar csar = ToscaContext.get().getArchive(name, version);
        if (csar != null) {
            newDependency.setHash(csar.getHash());
        }
        return newDependency;
    }
}
