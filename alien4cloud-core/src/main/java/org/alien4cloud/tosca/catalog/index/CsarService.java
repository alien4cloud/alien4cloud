package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.catalog.events.AfterArchiveDeleted;
import org.alien4cloud.tosca.catalog.events.ArchiveUsageRequestEvent;
import org.alien4cloud.tosca.catalog.events.BeforeArchiveDeleted;
import org.alien4cloud.tosca.catalog.repository.CsarFileRepository;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.Usage;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.utils.AlienConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages cloud services archives and their dependencies.
 */
@Component
@Slf4j
public class CsarService {
    @Inject
    private ApplicationEventPublisher publisher;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO csarDAO;
    @Inject
    private IToscaTypeIndexerService indexerService;
    @Inject
    private CsarFileRepository alienRepository;
    @Inject
    private ApplicationService applicationService;

    /**
     * Check if a given archive exists in any workspace.
     *
     * @param name The name of the archive.
     * @param version The version of the archive.
     * @return Return the matching
     */
    public boolean exists(String name, String version) {
        return csarDAO.buildQuery(Csar.class).setFilters(fromKeyValueCouples("version", version, "name", name)).count() > 0;
    }

    /**
     * Check that a CSAR name/version does not already exists in the repository and eventually throw an AlreadyExistException.
     *
     * @param name The name of the archive.
     * @param version The version of the archive.
     */
    public void ensureUniqueness(String name, String version) {
        if (exists(name, version)) {
            throw new AlreadyExistException("CSAR: " + name + ", Version: " + version + " already exists in the repository.");
        }
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

    /**
     * @return an array of CSARs that depend on this name:version.
     */
    public Csar[] getDependantCsars(String name, String version) {
        FilterBuilder notSelf = FilterBuilders
                .notFilter(FilterBuilders.andFilter(FilterBuilders.termFilter("name", name), FilterBuilders.termFilter("version", version)));
        GetMultipleDataResult<Csar> result = csarDAO.buildQuery(Csar.class).prepareSearch()
                .setFilters(fromKeyValueCouples("dependencies.name", name, "dependencies.version", version), notSelf).search(0, Integer.MAX_VALUE);
        return result.getData();
    }

    /**
     * Get teh topologies that depends on this csar.
     * Do not return a topology if this csar is his own
     *
     * @return an array of <code>Topology</code>s that depend on this name:version.
     */
    public Topology[] getDependantTopologies(String name, String version) {
        FilterBuilder notSelf = FilterBuilders
                .notFilter(FilterBuilders.andFilter(FilterBuilders.termFilter("archiveName", name), FilterBuilders.termFilter("archiveVersion", version)));

        GetMultipleDataResult<Topology> result = csarDAO.buildQuery(Topology.class).prepareSearch()
                .setFilters(fromKeyValueCouples("dependencies.name", name, "dependencies.version", version), notSelf).search(0, Integer.MAX_VALUE);
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
        GetMultipleDataResult<Location> result = csarDAO.buildQuery(Location.class)
                .setFilters(fromKeyValueCouples("dependencies.name", name, "dependencies.version", version)).prepareSearch().search(0, Integer.MAX_VALUE);
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
        this.csarDAO.save(csar);
    }

    /**
     * Set dependencies to an existing CSAR given its Id, and save it.
     * <p>
     * See {@link CsarService#setDependencies(String, Set)}
     * </p>
     *
     *
     * @param csarId id of the CSAR
     * @param dependencies the new dependencies
     */
    public void setDependencies(String csarId, Set<CSARDependency> dependencies) {
        Csar csar = getOrFail(csarId);
        setDependencies(csar, dependencies);
    }

    /**
     * Set the dependencies of a given csar to the provided set.
     * <p>
     * This method will remove ,if present, the provided <b>csar</b> from the provided set of <b>dependencies</b>, to avoid cyclic dependencies on itself.
     * </p>
     * Note that no saving operation is perform here
     *
     * @param csar: The csar we want to set the dependencies
     * @param dependencies The provided dependencies to use.
     */
    public void setDependencies(Csar csar, Set<CSARDependency> dependencies) {
        csar.setDependencies(remove(csar, dependencies));
        save(csar);
    }

    /**
     * remove a csar from a set of dependencies
     * 
     * @param csar
     * @param from
     * @return
     */
    private Set<CSARDependency> remove(Csar csar, Set<CSARDependency> from) {
        CSARDependency toRemove = new CSARDependency(csar.getName(), csar.getVersion());
        return from == null ? null : from.stream().filter(csarDependency -> !Objects.equals(toRemove, csarDependency)).collect(Collectors.toSet());
    }

    /**
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
        return getOrFail(Csar.createId(name, version));
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
        return topologies != null && topologies.length > 0;
    }

    /**
     * Delete an archive.
     * <p>
     * Unlike {@link CsarService#deleteCsar(String)}, the archive will be deleted regardless if it is used as a dependency somewhere.
     * </p>
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
     * @throws DeleteReferencedObjectException If the csar is a dependency of another csar or topology
     */
    public void deleteCsar(String csarId) {
        Csar csar = getOrFail(csarId);
        // a csar that is a dependency of another csar can not be deleted
        if (isDependency(csar.getName(), csar.getVersion())) {
            throw new DeleteReferencedObjectException("This csar can not be deleted since it's a dependencie for others");
        }

        deleteCsar(csar);
    }

    public void deleteCsar(Csar csar) {
        // dispatch event before indexing
        publisher.publishEvent(new BeforeArchiveDeleted(this, csar.getId()));

        deleteCsarContent(csar);
        csarDAO.delete(Csar.class, csar.getId());
        // physically delete files
        alienRepository.removeCSAR(csar.getName(), csar.getVersion());

        // dispatch event before indexing
        publisher.publishEvent(new AfterArchiveDeleted(this, csar.getId()));
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
        // if the csar is bound to an application, then do not allow the process
        if (Objects.equals(csar.getDelegateType(), ArchiveDelegateType.APPLICATION.toString())) {
            throw new UnsupportedOperationException("Cannot delete an application csar from here ");
        }
        List<Usage> relatedResourceList = getCsarRelatedResourceList(csar);
        if (relatedResourceList.isEmpty()) {
            deleteCsar(csar);
        }
        return relatedResourceList;
    }

    /**
     * Get the list of resources that are using the given archive.
     *
     * @param csar The archive for which to get usage.
     * @return The list of usage of the archive.
     */
    public List<Usage> getCsarRelatedResourceList(Csar csar) {
        if (csar == null) {
            log.debug("You have requested a resource list for a invalid csar object : <" + csar + ">");
            return Lists.newArrayList();
        }

        ArchiveUsageRequestEvent archiveUsageRequestEvent = new ArchiveUsageRequestEvent(this, csar.getName(), csar.getVersion());

        // Archive from applications are used by the application.
        if (Objects.equals(csar.getDelegateType(), ArchiveDelegateType.APPLICATION.toString())) {
            // The CSAR is from an application's topology
            Application application = applicationService.checkAndGetApplication(csar.getDelegateId());
            archiveUsageRequestEvent
                    .addUsage(new Usage(application.getName(), Application.class.getSimpleName().toLowerCase(), csar.getDelegateId(), csar.getWorkspace()));
        }

        // a csar that is a dependency of another csar can not be deleted
        Csar[] relatedCsars = getDependantCsars(csar.getName(), csar.getVersion());
        if (ArrayUtils.isNotEmpty(relatedCsars)) {
            archiveUsageRequestEvent.addUsages(generateCsarsInfo(relatedCsars));
        }

        // check if some of the nodes are used in topologies.
        Topology[] topologies = getDependantTopologies(csar.getName(), csar.getVersion());
        if (topologies != null && topologies.length > 0) {
            archiveUsageRequestEvent.addUsages(generateTopologiesInfo(topologies));
        }

        // a csar that is a dependency of location can not be deleted
        Location[] relatedLocations = getDependantLocations(csar.getName(), csar.getVersion());
        if (relatedLocations != null && relatedLocations.length > 0) {
            archiveUsageRequestEvent.addUsages(generateLocationsInfo(relatedLocations));
        }

        publisher.publishEvent(archiveUsageRequestEvent);

        return archiveUsageRequestEvent.getUsages();
    }

    /**
     * Generate resources related to a csar list
     *
     * @param csars
     * @return
     */
    public List<Usage> generateCsarsInfo(Csar[] csars) {
        String resourceName;
        String resourceId;
        List<Usage> resourceList = Lists.newArrayList();
        for (Csar csar : csars) {
            if (ArchiveDelegateType.APPLICATION.toString().equals(csar.getDelegateType())) {
                Application application = applicationService.checkAndGetApplication(csar.getDelegateId());
                resourceName = application.getName();
            } else {
                resourceName = csar.getName();
            }
            Usage temp = new Usage(resourceName, Csar.class.getSimpleName().toLowerCase(), csar.getId(), csar.getWorkspace());
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
    public List<Usage> generateLocationsInfo(Location[] locations) {
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
     * Generate resources (application or template) related to a topology list
     *
     * @param topologies
     * @return
     */
    public List<Usage> generateTopologiesInfo(Topology[] topologies) {
        List<Usage> resourceList = Lists.newArrayList();

        List<Csar> topologiesCsar = getTopologiesCsar(topologies);
        for (Csar csar : topologiesCsar) {
            if (Objects.equals(csar.getDelegateType(), ArchiveDelegateType.APPLICATION.toString())) {
                // get the related application
                Application application = applicationService.checkAndGetApplication(csar.getDelegateId());
                resourceList.add(new Usage(application.getName(), csar.getDelegateType(), csar.getDelegateId(), csar.getWorkspace()));
            } else {
                resourceList.add(new Usage(csar.getName() + "[" + csar.getVersion() + "]", "topologyTemplate", csar.getId(), csar.getWorkspace()));
            }
        }
        return resourceList;
    }
}
