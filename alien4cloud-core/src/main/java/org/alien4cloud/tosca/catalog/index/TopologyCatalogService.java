package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import alien4cloud.common.AlienConstants;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.utils.VersionUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for indexing and updating topologies.
 */
@Slf4j
@Service
public class TopologyCatalogService extends AbstractToscaIndexSearchService<Topology> implements ITopologyCatalogService {
    @Inject
    private ArchiveIndexer archiveIndexer;

    @Override
    public Topology createTopologyAsTemplate(String name, String description, String version, String workspace, String fromTopologyId) {
        if (!TopologyUtils.isValidNodeName(name)) {
            throw new InvalidNameException("topologyTemplateName", name,
                    "Topology template name <" + name + "> is not valid. It must not contains any special characters.");
        }
        // Every version of a topology template has a Cloud Service Archive
        Csar csar = new Csar(name, StringUtils.isNotBlank(version) ? version : VersionUtil.DEFAULT_VERSION_NAME);
        csar.setWorkspace(workspace);
        csar.setDelegateType(ArchiveDelegateType.CATALOG.toString());
        if (description == null) {
            csar.setDescription("This archive has been created with alien4cloud.");
        } else {
            csar.setDescription("Enclosing archive for topology " + description);
        }

        Topology topology;
        if (fromTopologyId != null) { // "cloning" the topology
            topology = alienDAO.findById(Topology.class, fromTopologyId);
        } else {
            topology = new Topology();
        }
        topology.setDescription(description);
        topology.setArchiveName(csar.getName());
        topology.setArchiveVersion(csar.getVersion());
        topology.setWorkspace(csar.getWorkspace());
        csar.setDependencies(topology.getDependencies());
        archiveIndexer.importNewArchive(csar, topology);
        return topology;
    }

    @Override
    protected Topology[] getArray(int size) {
        return new Topology[size];
    }

    @Override
    protected String getAggregationField() {
        return "archiveName";
    }

    // we need to override for aspect purpose
    @Override
    public FacetedSearchResult search(Class<? extends Topology> clazz, String query, Integer size, Map<String, String[]> filters) {
        return super.search(clazz, query, size, filters);
    }

    @Override
    public Topology[] getAll(Map<String, String[]> filters, String archiveName) {
        return alienDAO.buildQuery(Topology.class)
                .setFilters(fromKeyValueCouples(filters, "workspace", AlienConstants.GLOBAL_WORKSPACE_ID, "archiveName", archiveName)).prepareSearch()
                .setFetchContext(SUMMARY).search(0, Integer.MAX_VALUE).getData();
    }

    @Override
    public Topology getOrFail(String id) {
        Topology topology = get(id);
        if (topology == null) {
            throw new NotFoundException("Unable to find a topology with id <" + id + ">");
        }
        return topology;
    }

    @Override
    public Topology get(String id) {
        return alienDAO.findById(Topology.class, id);
    }

    /**
     * Return true if the given id exists.
     *
     * @param id The id to check.
     * @return True if a topology with the given id exists, false if not.
     */
    @Override
    public boolean exists(String id) {
        return alienDAO.exist(Topology.class, id);
    }
}
