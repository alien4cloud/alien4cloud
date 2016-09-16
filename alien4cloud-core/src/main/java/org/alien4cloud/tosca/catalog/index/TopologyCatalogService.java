package org.alien4cloud.tosca.catalog.index;

import alien4cloud.common.AlienConstants;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.Version;
import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

/**
 * Service responsible for indexing and updating topologies.
 */
@Service
public class TopologyCatalogService extends AbstractToscaIndexSearchService<Topology> {

    @Override
    protected Topology[] getArray(int size) {
        return new Topology[size];
    }

    @Override
    protected String getAggregationField() {
        return "archiveName";
    }

    /**
     * Creates a topology and register it as a template in the catalog
     *
     * @param name        The name of the topology template
     * @param description The description of the topology template
     * @param version     The version of the topology
     * @return The @{@link Topology} newly created
     */
    public Topology createTopologyAsTemplate(String name, String description, String version) {
        Topology topology = new Topology();

        // Every version of a topology template has a Cloud Service Archive
        String delegateType = ArchiveDelegateType.CATALOG.toString();
        Csar csar = new Csar(name, StringUtils.isNotBlank(version) ? version : VersionUtil.DEFAULT_VERSION_NAME);
        csar.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        csar.setDelegateType(delegateType);
        ensureUniqueness(Csar.class, csar.getId());

        topology.setArchiveName(csar.getName());
        topology.setArchiveVersion(csar.getVersion());
        topology.setNestedVersion(new Version(topology.getArchiveVersion()));
        topology.setWorkspace(csar.getWorkspace());
        ensureUniqueness(Topology.class, topology.getId());

        alienDAO.save(csar);
        alienDAO.save(topology);

        return topology;
    }

    private void ensureUniqueness(Class<?> clazz, String id) {
        long count = alienDAO.count(clazz, QueryBuilders.idsQuery().ids(id));
        if (count > 0) {
            throw new AlreadyExistException("Object already exist");
        }
    }
}
