package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;

import java.util.Map;

import alien4cloud.common.AlienConstants;
import org.alien4cloud.tosca.model.templates.Topology;
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
     * Get all topologies matching the given set of filters.
     *
     * @param filters The filters to query the topologies.
     * @return Return the matching
     */
    public Topology[] getAll(Map<String, String[]> filters, String archiveName) {
        return searchDAO.buildQuery(Topology.class)
                .setFilters(fromKeyValueCouples(filters, "workspace", AlienConstants.GLOBAL_WORKSPACE_ID, "archiveName", archiveName)).prepareSearch()
                .search(0, Integer.MAX_VALUE).getData();
    }
}