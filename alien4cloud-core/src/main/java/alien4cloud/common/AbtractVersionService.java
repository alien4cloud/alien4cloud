package alien4cloud.common;

public abstract class AbtractVersionService {

    // /**
    // * Get the version from a topology id
    // *
    // * @param topologyId The id of the application for which to get environments.
    // * @return An array of the applications/topology templates versions for the requested topology id.
    // */
    // public V getByTopologyId(String topologyId) {
    // GetMultipleDataResult<V> result = alienDAO.find(getVersionImplemClass(),
    // MapUtil.newHashMap(new String[] { "topologyId" }, new String[][] { new String[] { topologyId } }), Integer.MAX_VALUE);
    // return (result.getData() == null || result.getData().length != 1) ? null : result.getData()[0];
    // }
    //
    // /**
    // * Get all versions snapshot for a given application/topology template.
    // *
    // * @param delegateId The id of the application/topology template for which to get versions.
    // * @return An array of the applications/topology templates versions snapshot for the requested application id.
    // */
    // public V[] getSnapshotByDelegateId(String delegateId) {
    // GetMultipleDataResult<V> result = alienDAO.find(getVersionImplemClass(), MapUtil.newHashMap(new String[] { getDelegatePropertyName(), "isSnapshot" },
    // new String[][] { new String[] { delegateId }, new String[] { "true" } }), Integer.MAX_VALUE);
    // return result.getData();
    // }
    //
    //
    // /**
    // * Delete a version and the related topologies. Fail if version doesn't exist.
    // *
    // * @param id The id of the version to delete.
    // */
    // public void delete(String id) {
    // V version = this.getOrFail(id);
    // deleteVersion(version);
    // }
    //
    //
    //
    //
    // /**
    // * Get an application/topology template version by id or fail if not found.
    // *
    // * @param id
    // * @return the version or throw an exception
    // */
    // public V getOrFail(String id) {
    // V v = alienDAO.findById(getVersionImplemClass(), id);
    // if (v == null) {
    // throw new NotFoundException("Version with id <" + id + "> does not exist");
    // }
    // return v;
    // }
    //
    // /**
    // * Get a version by id.
    // *
    // * @param id
    // * @return the version or null if not found
    // */
    // public V get(String id) {
    // return alienDAO.findById(getVersionImplemClass(), id);
    // }
    //
    // /**
    // * Sort the versions from newest to oldest.
    // *
    // * @param data
    // * @return a sorted array of versions
    // */
    // public V[] sortArrayOfVersion(V[] data) {
    // if (data == null || data.length <= 1) {
    // return data;
    // }
    // List<V> sortedData = Lists.newArrayList(data);
    // Collections.sort(sortedData, new Comparator<V>() {
    // @Override
    // public int compare(V left, V right) {
    // return VersionUtil.compare(right.getVersion(), left.getVersion());
    // }
    // });
    // return sortedData.toArray(buildVersionImplemArray(data.length));
    // }
    //
    // /**
    // * Filter to search app/tt versions only for an delegate id.
    // *
    // * @param delegateId
    // * @return a filter for application/tt versions
    // */
    // public Map<String, String[]> getVersionsFilters(String delegateId, String version) {
    // List<String> filterKeys = Lists.newArrayList();
    // List<String[]> filterValues = Lists.newArrayList();
    // if (delegateId != null) {
    // filterKeys.add(getDelegatePropertyName());
    // filterValues.add(new String[] { delegateId });
    // }
    // if (version != null && !version.equals("")) {
    // filterKeys.add("version");
    // filterValues.add(new String[] { version });
    // }
    // return MapUtil.newHashMap(filterKeys.toArray(new String[filterKeys.size()]), filterValues.toArray(new String[filterValues.size()][]));
    // }
}