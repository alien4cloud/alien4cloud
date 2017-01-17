package alien4cloud.application;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.utils.AlienConstants.APP_WORKSPACE_PREFIX;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.utils.version.InvalidVersionException;
import alien4cloud.utils.version.UpdateApplicationVersionException;
import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.catalog.index.ArchiveIndexer;
import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteLastApplicationVersionException;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.exception.ReleaseReferencingSnapshotException;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.VersionUtil;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Service
public class ApplicationVersionService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ArchiveIndexer archiveIndexer;
    @Inject
    private CsarService csarService;
    @Inject
    private TopologyServiceCore topologyServiceCore;

    /**
     * Create a new version for an application based on an existing topology with the default version name.
     *
     * @param applicationId The id of the application for which to create the version.
     * @param topologyId The id of the topology to clone for the version's topology.
     */
    public ApplicationVersion createInitialVersion(String applicationId, String topologyId) {
        return createApplicationVersion(applicationId, VersionUtil.DEFAULT_VERSION_NAME, null, topologyId, false);
    }

    /**
     * Create a new version for an application. The new application version can be created from an exiting application version of not.
     * When created from an existing application version all topology versions from the original version will be created in the new application version.
     *
     * @param applicationId The id of the application for which to create the version.
     * @param version The new version.
     * @param description The description.
     * @param originalId The version (application version or topology) from witch to create the new application version.
     * @param originalIsAppVersion True if the originalId is the id of an application version id, false if it is the id of a topology id.
     */
    public ApplicationVersion createApplicationVersion(String applicationId, String version, String description, String originalId,
            boolean originalIsAppVersion) {
        if (isVersionNameExist(applicationId, version)) {
            throw new AlreadyExistException("A version " + version + " already exists for application " + applicationId + ".");
        }

        ApplicationVersion appVersion = new ApplicationVersion();
        appVersion.setDelegateId(applicationId);
        appVersion.setVersion(version);
        appVersion.setNestedVersion(VersionUtil.parseVersion(version));
        appVersion.setReleased(!VersionUtil.isSnapshot(version));
        appVersion.setDescription(description);
        appVersion.setTopologyVersions(Maps.newHashMap());

        // Create all topology versions based on the previous version configuration
        if (originalIsAppVersion && originalId != null) {
            ApplicationVersion originalAppVersion = getOrFail(originalId);
            // Ensure that the id of the original application version is indeed the same as the one of the application
            if (!applicationId.equals(originalAppVersion.getApplicationId())) {
                throw new AuthorizationServiceException("Creating a new version of an application from the version of another application is not authorized.");
            }

            // if the version is a release version we have to ensure that every sub-topology can be released
            importTopologiesFromPreviousVersion(appVersion, originalAppVersion);
        } else {
            // Don't create the application version from an existing version but from either a topology template or start a blank topology
            Topology topology;
            if (originalId == null) {
                topology = new Topology();
            } else {
                topology = getTemplateTopology(originalId);
            }
            // Create a default topology version for this application version.
            ApplicationTopologyVersion applicationTopologyVersion = createTopologyVersion(applicationId, version, null, "default topology", topology);
            appVersion.getTopologyVersions().put(version, applicationTopologyVersion);
        }

        // Save the version object
        alienDAO.save(appVersion);

        return appVersion;
    }

    private void importTopologiesFromPreviousVersion(ApplicationVersion newVersion, ApplicationVersion originalAppVersion) {
        // if the version is a release version we have to ensure that every sub-topology can be released
        Map<String, Topology> previousTopologies = Maps.newHashMap();
        // If the previous version was not a release and we want to create a release version then we have to check that all topologies can indeed be released.
        boolean mustCheckReleasable = newVersion.isReleased() && !originalAppVersion.isReleased();

        for (ApplicationTopologyVersion originalAppTopoVersion : originalAppVersion.getTopologyVersions().values()) {
            // Ensure that all versions can be created and does not exists already.
            csarService.ensureUniqueness(newVersion.getApplicationId(), newVersion.getVersion() + "-" + originalAppTopoVersion.getQualifier());
            // Cache the previous topology for duplication when creating the new version.
            Topology topology = alienDAO.findById(Topology.class, originalAppTopoVersion.getArchiveId());
            if (mustCheckReleasable) {
                // If the new version is a release, we have to ensure that all dependencies are released
                checkTopologyReleasable(topology);
            }
            previousTopologies.put(originalAppTopoVersion.getArchiveId(), topology);
        }

        for (ApplicationTopologyVersion originalAppTopoVersion : originalAppVersion.getTopologyVersions().values()) {
            String newTopologyVersion = getTopologyVersion(newVersion.getVersion(), originalAppTopoVersion.getQualifier());
            ApplicationTopologyVersion applicationTopologyVersion = createTopologyVersion(newVersion.getApplicationId(), newTopologyVersion,
                    originalAppTopoVersion.getQualifier(), originalAppTopoVersion.getDescription(),
                    previousTopologies.get(originalAppTopoVersion.getArchiveId()));
            // Add the newly created application version to the list.
            newVersion.getTopologyVersions().put(newTopologyVersion, applicationTopologyVersion);
        }
    }

    /**
     * Create a new application topology version. If originalId is not null the topology will be created:
     * - From a topology template if originalIsVersion is false
     * - From a version of the application if
     *
     * @param applicationId The id of the application for which to create the new application topology version.
     * @param versionId The id of the version for which to create a new topology version.
     * @param qualifier The qualifier for this specific topology version.
     * @param description The description of the topology version.
     * @param originalId The id that points to a topology used to initialize the new application topology version.
     * @param originalIsVersion True if the originalId is the id of an application topology version, false if this is the id of a topology template.
     */
    public void createTopologyVersion(String applicationId, String versionId, String qualifier, String description, String originalId,
            boolean originalIsVersion) {
        // Get the version from elastic-search
        ApplicationVersion applicationVersion = getOrFail(versionId);
        Topology topology;
        if (originalId == null) {
            topology = new Topology();
        } else {
            if (originalIsVersion) {
                // we need to check that the version is indeed a previous version of this application
                ApplicationVersion originalVersion = getOrFailByArchiveId(originalId);
                if (!applicationId.equals(originalVersion.getApplicationId())) {
                    throw new AuthorizationServiceException(
                            "Creating a new version of an application from the version of another application is not authorized.");
                }
                topology = topologyServiceCore.getOrFail(originalId);
            } else {
                topology = getTemplateTopology(originalId);
            }
        }

        String version = getTopologyVersion(applicationVersion.getVersion(), qualifier);
        if (applicationVersion.getTopologyVersions().get(version) != null) {
            throw new AlreadyExistException("The topology version <" + versionId + "> already exists for application <" + applicationId + ">");
        }
        // Create a new application version based on an existing topology.
        ApplicationTopologyVersion applicationTopologyVersion = createTopologyVersion(applicationId, version, qualifier, description, topology);
        applicationVersion.getTopologyVersions().put(version, applicationTopologyVersion);
        alienDAO.save(applicationVersion);
    }

    private String getTopologyVersion(String version, String qualifier) {
        if (qualifier == null || qualifier.isEmpty()) {
            return version;
        }
        int snapshotIndex = version.indexOf("-SNAPSHOT");
        String prefix = version;
        String suffix = "";
        if (snapshotIndex > 0) {
            prefix = version.substring(0, snapshotIndex) + version.substring(snapshotIndex + 9, version.length());
            suffix = "-SNAPSHOT";
        }
        return prefix + "-" + qualifier + suffix;
    }

    /**
     * Get the topology from a Topology Template and NOT from an application.
     * 
     * @return The topology.
     * @throws AccessDeniedException in case the topology is not from a template.
     */
    private Topology getTemplateTopology(String archiveId) {
        // We need to ensure that the topology is indeed a topology template
        if (alienDAO.buildQuery(ApplicationVersion.class).setFilters(fromKeyValueCouples("topologyVersions.value.archiveId", archiveId)).count() > 0) {
            throw new AccessDeniedException(
                    "Creation of a new application topology version from a topology template should not be done using the id of the topology of an application.");
        }
        return topologyServiceCore.getOrFail(archiveId);
    }

    private ApplicationTopologyVersion createTopologyVersion(String applicationId, String version, String qualifier, String description, Topology topology) {
        // Every version of an application has a Cloud Service Archive
        String delegateType = ArchiveDelegateType.APPLICATION.toString();
        Csar csar = new Csar(applicationId, version);
        csar.setWorkspace(APP_WORKSPACE_PREFIX + ":" + applicationId);
        csar.setDelegateId(applicationId);
        csar.setDelegateType(delegateType);

        topology.setArchiveName(csar.getName());
        topology.setArchiveVersion(csar.getVersion());
        topology.setWorkspace(csar.getWorkspace());

        // first of all, if the new version is a release, we have to ensure that all dependencies are released
        if (!VersionUtil.isSnapshot(version)) {
            checkTopologyReleasable(topology);
        }

        // Import the created archive and topology
        archiveIndexer.importNewArchive(csar, topology);

        ApplicationTopologyVersion applicationTopologyVersion = new ApplicationTopologyVersion();
        applicationTopologyVersion.setArchiveId(csar.getId());
        applicationTopologyVersion.setQualifier(qualifier);
        applicationTopologyVersion.setDescription(description);

        return applicationTopologyVersion;
    }

    /**
     * Check that the topology can be associated to a release version, actually : check that the topology doesn't reference SNAPSHOT
     * dependencies.
     *
     * @throws @{@link ReleaseReferencingSnapshotException} if the topology references SNAPSHOT dependencies
     *             version.
     */
    private void checkTopologyReleasable(Topology topology) {
        if (topology.getDependencies() != null) {
            for (CSARDependency dep : topology.getDependencies()) {
                // we allow SNAPSHOTS only for tosca-normative-types (we don't expect to have a release soon !)
                if (VersionUtil.isSnapshot(dep.getVersion()) && !dep.getName().equals("tosca-normative-types")) {
                    throw new ReleaseReferencingSnapshotException(String.format("Can not release: %s dependency is a snapshot", dep.getName()));
                }
            }
        }
    }

    /**
     * Check uniqueness of a version for a given application/topology template.
     *
     * @param delegateId
     * @param versionName
     * @return a boolean indicating if the version exists.
     */
    private boolean isVersionNameExist(String delegateId, String versionName) {
        long matchCount = alienDAO.buildQuery(ApplicationVersion.class).setFilters(fromKeyValueCouples("applicationId", delegateId, "version", versionName))
                .count();
        return matchCount > 0;
    }

    /**
     * Get all versions for a given delegate.
     *
     * @param delegateId The id of the application for which to get environments.
     * @return An array of the applications versions for the requested application id.
     */
    public ApplicationVersion[] getByApplicationId(String delegateId) {
        GetMultipleDataResult<ApplicationVersion> result = alienDAO.find(ApplicationVersion.class, fromKeyValueCouples("applicationId", delegateId),
                Integer.MAX_VALUE);
        return result.getData();
    }

    /**
     * Get the latest snapshot version for a given application.
     * 
     * @param applicationId The id of the application for which to get the latest snapshot.
     * @return The latest snapshot version for a given application.
     */
    public ApplicationVersion getLatestSnapshot(String applicationId) {
        return alienDAO.buildQuery(ApplicationVersion.class).prepareSearch()
                .setFilters(fromKeyValueCouples("applicationId", applicationId, "released", "false"))
                .alterSearchRequestBuilder(searchRequestBuilder -> addSort(searchRequestBuilder)).find();
    }

    /**
     * Search for versions.
     * 
     * @param applicationId The id of the application for which to search for versions.
     * @param query The query text to find a given version.
     * @param from The index from which to get versions.
     * @param size The size of the query.
     * @return A GetMultipleDataResult that contains the ApplicationVersion as data.
     */
    public GetMultipleDataResult<ApplicationVersion> search(String applicationId, String query, int from, int size) {
        return alienDAO.buildSearchQuery(ApplicationVersion.class, query).prepareSearch().setFilters(fromKeyValueCouples("applicationId", applicationId))
                .alterSearchRequestBuilder(searchRequestBuilder -> addSort(searchRequestBuilder)).search(from, size);
    }

    private void addSort(SearchRequestBuilder searchRequestBuilder) {
        searchRequestBuilder.addSort(new FieldSortBuilder("nestedVersion.majorVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.minorVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.incrementalVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.qualifier").order(SortOrder.DESC).missing("_first"));
    }

    /**
     * Delete all versions related to an application.
     *
     * @param applicationId The application id.
     */
    public void deleteByApplication(String applicationId) {
        ApplicationVersion[] versions = getByApplicationId(applicationId);
        for (ApplicationVersion version : versions) {
            deleteVersion(version);
        }
    }

    /**
     * Delete a specific application version.
     * 
     * @param applicationVersionId The id of the specific version to delete.
     */
    public void delete(String applicationVersionId) {
        ApplicationVersion applicationVersion = getOrFail(applicationVersionId);
        // check that this is not the last version
        if (alienDAO.buildQuery(ApplicationVersion.class).setFilters(fromKeyValueCouples("applicationId", applicationVersion.getApplicationId()))
                .count() == 1) {
            throw new DeleteLastApplicationVersionException(
                    "Application version with id <" + applicationVersionId + "> can't be be deleted as it is the last application version.");
        }
        // check that the version is not deployed
        if (isApplicationVersionDeployed(applicationVersion)) {
            throw new DeleteReferencedObjectException("Application version with id <" + applicationVersionId + "> could not be deleted as it is used");
        }
        deleteVersion(applicationVersion);
    }

    private void deleteVersion(ApplicationVersion version) {
        // Call delete archive
        for (ApplicationTopologyVersion topologyVersion : version.getTopologyVersions().values()) {
            csarService.deleteCsar(topologyVersion.getArchiveId());
        }
        alienDAO.delete(ApplicationVersion.class, version.getId());
    }

    /**
     * Delete an Application Topology version from a given application version.
     *
     * @param applicationId The application Id.
     * @param versionId The version id of the application version.
     * @param topologyVersion The version (not archive id) of the topology version.
     */
    public void deleteTopologyVersion(String applicationId, String versionId, String topologyVersion) {
        ApplicationVersion applicationVersion = getOrFail(versionId);
        ApplicationTopologyVersion applicationTopologyVersion = applicationVersion.getTopologyVersions().get(topologyVersion);
        if (applicationTopologyVersion == null) {
            throw new NotFoundException("Topology version <" + topologyVersion + "> does not exist in the application version <" + versionId
                    + "> for application <" + applicationId + ">");
        }

        if (applicationVersion.getTopologyVersions().size() == 1) {
            throw new DeleteLastApplicationVersionException("Application topology version <" + topologyVersion + "> for application version <" + versionId
                    + "> can't be be deleted as it is the last topology version for this application version.");
        }

        if (isApplicationTopologyVersionDeployed(applicationTopologyVersion)) {
            throw new DeleteReferencedObjectException("Application topology version with id <" + topologyVersion + "> could not be deleted as it is used");
        }

        ApplicationTopologyVersion deleted = applicationVersion.getTopologyVersions().remove(topologyVersion);
        csarService.deleteCsar(deleted.getArchiveId());
        alienDAO.save(applicationVersion);
    }

    /**
     * Update an application version and all it's topology versions.
     *
     * @param applicationVersionId The application version for which to update all versions.
     * @param newVersion The updated version number.
     * @param newDescription The new description.
     */
    public void update(String applicationId, String applicationVersionId, String newVersion, String newDescription) {
        ApplicationVersion applicationVersion = getOrFail(applicationVersionId);

        if (!applicationId.equals(applicationVersion.getApplicationId())) {
            throw new AuthorizationServiceException("It is not authorize to change an application with a wrong application id: request application id <"
                    + applicationId + "> version application id: <" + applicationVersion.getApplicationId() + ">");
        }

        if (newDescription != null) {
            applicationVersion.setDescription(newDescription);
        }

        if (newVersion != null && !applicationVersion.getVersion().equals(newVersion)) {
            // if the version is a release it is not possible to change it's version number
            if (applicationVersion.isReleased()) {
                throw new UpdateApplicationVersionException("The application version " + applicationVersion.getId() + " is released and cannot be update.");
            }

            if (isApplicationVersionNameExist(applicationVersion.getApplicationId(), newVersion)) {
                throw new AlreadyExistException("An application version already exist for this application with the version :" + newVersion);
            }

            // it is not possible to update the version number of a deployed version
            if (isApplicationVersionDeployed(applicationVersion)) {
                throw new DeleteReferencedObjectException("Application version with id <" + applicationVersionId + "> could not be deleted as it is used");
            }

            // When changing the version number we actually perform a full version creation and then delete the previous one.
            ApplicationVersion newApplicationVersion = new ApplicationVersion();
            newApplicationVersion.setVersion(newVersion);
            newApplicationVersion.setNestedVersion(VersionUtil.parseVersion(newVersion));
            newApplicationVersion.setDescription(applicationVersion.getDescription());
            newApplicationVersion.setApplicationId(applicationVersion.getApplicationId());
            newApplicationVersion.setReleased(!VersionUtil.isSnapshot(newVersion));
            newApplicationVersion.setTopologyVersions(Maps.newHashMap());

            importTopologiesFromPreviousVersion(newApplicationVersion, applicationVersion);

            // save the new version
            alienDAO.save(newApplicationVersion);
            // delete the previous version
            deleteVersion(applicationVersion);
        } else {
            // save the new version
            alienDAO.save(applicationVersion);
        }
    }

    /**
     * Get an application/topology template version by id.
     *
     * @param id The id of the application version to get.
     * @return the version or null if it does not exists
     */
    public ApplicationVersion get(String id) {
        return alienDAO.findById(ApplicationVersion.class, id);
    }

    /**
     * Get an application/topology template version by id or fail if not found.
     *
     * @param id The id of the application version to get.
     * @return the version or throw an exception
     * @throws NotFoundException in case no version can be found for the given id.
     */
    public ApplicationVersion getOrFail(String id) {
        ApplicationVersion v = get(id);
        if (v == null) {
            throw new NotFoundException("Version with id <" + id + "> does not exist");
        }
        return v;
    }

    /**
     * Get the application version for the given application based on the id of the related topology archive.
     *
     * @param archiveId The id of the TOSCA archive / topology version for which to find the Application Version.
     * @return The application version.
     */
    public ApplicationVersion getOrFailByArchiveId(String archiveId) {
        ApplicationVersion v = alienDAO.buildQuery(ApplicationVersion.class).setFilters(fromKeyValueCouples("topologyVersions.value.archiveId", archiveId))
                .prepareSearch().find();
        if (v == null) {
            throw new NotFoundException("No application version found for archive <" + archiveId + ">");
        }
        return v;
    }

    /**
     * Get a specific topology version from an application version.
     * 
     * @param applicationVersionId The id of the application version.
     * @param topologyVersion The topology version.
     * @return The ApplicationTopologyVersion.
     * @Throws NotFoundException in case the application version or the topology version does not exists.
     */
    public ApplicationTopologyVersion getOrFail(String applicationVersionId, String topologyVersion) {
        ApplicationTopologyVersion v = getOrFail(applicationVersionId).getTopologyVersions().get(topologyVersion);
        if (v == null) {
            throw new NotFoundException("Topology Version with id <" + topologyVersion + "> does not exist");
        }
        return v;
    }

    /**
     * Check if any of the application topology version of a application version is deployed.
     *
     * @param applicationVersion The application version for which to check deployment.
     * @return true if the version is deployed, false if not.
     */
    public boolean isApplicationVersionDeployed(ApplicationVersion applicationVersion) {
        String[] topologyVersionsIds = applicationVersion.getTopologyVersions().values().stream()
                .map(applicationTopologyVersion -> applicationTopologyVersion.getArchiveId()).toArray(String[]::new);
        Map<String, String[]> versionsFilter = MapUtil.newHashMap(new String[] { "versionId" }, new String[][] { topologyVersionsIds });
        return alienDAO.buildQuery(Deployment.class).setFilters(fromKeyValueCouples(versionsFilter, "endDate", null)).count() > 0;
    }

    /**
     * Check if a specific application topology version is deployed.
     * 
     * @param applicationTopologyVersion
     * @return
     */
    public boolean isApplicationTopologyVersionDeployed(ApplicationTopologyVersion applicationTopologyVersion) {
        return alienDAO.buildQuery(Deployment.class).setFilters(fromKeyValueCouples("versionId", applicationTopologyVersion.getArchiveId(), "endDate", null))
                .count() > 0;
    }

    /**
     * Check if a name version is already use by an other application version is a specific application.
     * 
     * @param applicationId
     * @param applicationVersionName
     * @return isUsed A boolean.
     */
    public boolean isApplicationVersionNameExist(String applicationId, String applicationVersionName) {
        return isVersionNameExist(applicationId, applicationVersionName);
    }
}