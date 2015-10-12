package alien4cloud.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.exception.ReleaseReferencingSnapshotException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.AbstractTopologyVersion;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.VersionUtil;

import com.google.common.collect.Lists;

public abstract class AbtractVersionService<V extends AbstractTopologyVersion> {
    protected static final String DEFAULT_VERSION_NAME = "0.1.0-SNAPSHOT";

    @Resource(name = "alien-es-dao")
    protected IGenericSearchDAO alienDAO;

    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    protected abstract V buildVersionImplem();

    protected abstract V[] buildVersionImplemArray(int length);

    protected abstract Class<V> getVersionImplemClass();

    protected abstract Class<?> getDelegateClass();

    protected abstract String getDelegatePropertyName();

    /**
     * Create a new version for an application/topology template based on an existing topology with the default version name.
     *
     * @param delegateId The id of the application for which to create the version.
     * @param topologyId The id of the topology to clone for the version's topology.
     */
    public V createVersion(String delegateId, String topologyToCloneId, Topology topology) {
        return createVersion(delegateId, topologyToCloneId, DEFAULT_VERSION_NAME, null, topology);
    }

    /**
     * Create a new version for an application/topology template based on an existing topology.
     *
     * @param delegateId The id of the application/topology template for which to create the version.
     * @param topologyToCloneId The id of the topology to clone for the version's topology.
     * @param version The number version of the new application version.
     */
    public V createVersion(String delegateId, String topologyToCloneId, String version, String desc, Topology providedTopology) {
        if (isVersionNameExist(delegateId, version)) {
            throw new AlreadyExistException("An version " + version + " already exists.");
        }

        VersionUtil.parseVersion(version);
        V appVersion = buildVersionImplem();
        appVersion.setId(UUID.randomUUID().toString());
        appVersion.setDelegateId(delegateId);
        appVersion.setVersion(version);
        appVersion.setLatest(true);
        appVersion.setSnapshot(VersionUtil.isSnapshot(version));
        appVersion.setReleased(!VersionUtil.isSnapshot(version));
        appVersion.setDescription(desc);

        Topology topology = null;
        if (providedTopology != null) {
            topology = providedTopology;
        } else {
            if (topologyToCloneId != null) { // "cloning" the topology
                topology = alienDAO.findById(Topology.class, topologyToCloneId);
            } else {
                topology = new Topology();
            }
            topology.setId(UUID.randomUUID().toString());
        }
        topology.setDelegateId(delegateId);
        topology.setDelegateType(getDelegateClass().getSimpleName().toLowerCase());
        workflowBuilderService.initWorkflows(workflowBuilderService.buildTopologyContext(topology));
        // first of all, if the new version is a release, we have to ensure that all dependencies are released
        if (!VersionUtil.isSnapshot(version)) {
            checkTopologyReleasable(topology);
        }

        alienDAO.save(topology);

        appVersion.setTopologyId(topology.getId());
        alienDAO.save(appVersion);
        return appVersion;
    }

    /**
     * Check that the topology can be associated to a release version, actually : check that the topology doesn't reference SNAPSHOT
     * dependencies.
     * 
     * @throws a @{@link ReleaseReferencingSnapshotException} if the topology references SNAPSHOT dependencies
     *             version.
     */
    public void checkTopologyReleasable(Topology topology) {
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
     * Get all versions for a given delegate.
     *
     * @param applicationId The id of the application for which to get environments.
     * @return An array of the applications versions for the requested application id.
     */
    public V[] getByDelegateId(String delegateId) {
        GetMultipleDataResult<V> result = alienDAO.find(getVersionImplemClass(),
                MapUtil.newHashMap(new String[] { getDelegatePropertyName() }, new String[][] { new String[] { delegateId } }), Integer.MAX_VALUE);
        return result.getData();
    }

    /**
     * Get the version from a topology id
     *
     * @param topologyId The id of the application for which to get environments.
     * @return An array of the applications/topology templates versions for the requested topology id.
     */
    public V getByTopologyId(String topologyId) {
        GetMultipleDataResult<V> result = alienDAO.find(getVersionImplemClass(),
                MapUtil.newHashMap(new String[] { "topologyId" }, new String[][] { new String[] { topologyId } }), Integer.MAX_VALUE);
        return (result.getData() == null || result.getData().length != 1) ? null : result.getData()[0];
    }

    /**
     * Get all versions snapshot for a given application/topology template.
     *
     * @param delegateId The id of the application/topology template for which to get versions.
     * @return An array of the applications/topology templates versions snapshot for the requested application id.
     */
    public V[] getSnapshotByDelegateId(String delegateId) {
        GetMultipleDataResult<V> result = alienDAO.find(getVersionImplemClass(),
                MapUtil.newHashMap(new String[] { getDelegatePropertyName(), "isSnapshot" }, new String[][] { new String[] { delegateId },
                        new String[] { "true" } }),
                Integer.MAX_VALUE);
        return result.getData();
    }

    private void deleteVersion(V version) {
        alienDAO.delete(Topology.class, version.getTopologyId());
        alienDAO.delete(getVersionImplemClass(), version.getId());
    }

    /**
     * Delete a version and the related topologies. Fail if version doesn't exist.
     *
     * @param id The id of the version to delete.
     */
    public void delete(String id) {
        V version = this.getOrFail(id);
        deleteVersion(version);
    }

    /**
     * Delete all versions related to an application/topology template.
     *
     * @param delegateId The application/topology template id.
     */
    public void deleteByDelegate(String delegateId) {
        V[] versions = getByDelegateId(delegateId);
        for (V version : versions) {
            deleteVersion(version);
        }
    }

    /**
     * Check uniqueness of a version for a given application/topology template.
     * 
     * @param delegateId
     * @param versionName
     * @return a boolean indicating if the version exists.
     */
    public boolean isVersionNameExist(String delegateId, String versionName) {
        GetMultipleDataResult<V> dataResult = alienDAO.search(
                getVersionImplemClass(),
                null,
                MapUtil.newHashMap(new String[] { getDelegatePropertyName(), "version" }, new String[][] { new String[] { delegateId },
                        new String[] { versionName } }), 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return true;
        }
        return false;
    }

    /**
     * Get an application/topology template version by id or fail if not found.
     * 
     * @param id
     * @return the version or throw an exception
     */
    public V getOrFail(String id) {
        V v = alienDAO.findById(getVersionImplemClass(), id);
        if (v == null) {
            throw new NotFoundException("Version with id <" + id + "> does not exist");
        }
        return v;
    }

    /**
     * Get a version by id.
     * 
     * @param id
     * @return the version or null if not found
     */
    public V get(String id) {
        return alienDAO.findById(getVersionImplemClass(), id);
    }

    /**
     * Get a version for an application/topology template (returns the default if not found).
     * 
     * @param delegateId
     * @param versionId
     * @return
     */
    public V getVersionByIdOrDefault(String delegateId, String versionId) {
        V version = null;
        if (versionId == null) {
            V[] versions = getByDelegateId(delegateId);
            version = versions[0];
        } else {
            version = getOrFail(versionId);
        }
        return version;
    }

    /**
     * Sort the versions from newest to oldest.
     *
     * @param data
     * @return a sorted array of versions
     */
    public V[] sortArrayOfVersion(V[] data) {
        if (data == null || data.length <= 1) {
            return data;
        }
        List<V> sortedData = Lists.newArrayList(data);
        Collections.sort(sortedData, new Comparator<V>() {
            @Override
            public int compare(V left, V right) {
                return VersionUtil.compare(right.getVersion(), left.getVersion());
            }
        });
        return sortedData.toArray(buildVersionImplemArray(data.length));
    }

    /**
     * Filter to search app/tt versions only for an delegate id.
     *
     * @param delegateId
     * @return a filter for application/tt versions
     */
    public Map<String, String[]> getVersionsFilters(String delegateId, String version) {
        List<String> filterKeys = Lists.newArrayList();
        List<String[]> filterValues = Lists.newArrayList();
        if (delegateId != null) {
            filterKeys.add(getDelegatePropertyName());
            filterValues.add(new String[] { delegateId });
        }
        if (version != null && !version.equals("")) {
            filterKeys.add("version");
            filterValues.add(new String[] { version });
        }
        return MapUtil.newHashMap(filterKeys.toArray(new String[filterKeys.size()]), filterValues.toArray(new String[filterValues.size()][]));
    }

    public V searchByDelegateAndVersion(String delegateId, String version) {
        GetMultipleDataResult<V> result = alienDAO.find(getVersionImplemClass(), getVersionsFilters(delegateId, version), Integer.MAX_VALUE);
        if (result.getTotalResults() > 0) {
            return result.getData()[0];
        }
        return null;
    }

    public void changeTopology(TopologyTemplateVersion topologyTemplateVersion, String topologyId) {
        String oldTopologyId = topologyTemplateVersion.getTopologyId();
        topologyTemplateVersion.setTopologyId(topologyId);
        alienDAO.save(topologyTemplateVersion);
        alienDAO.delete(Topology.class, oldTopologyId);
    }

}
