package org.alien4cloud.tosca.catalog.index;

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
}
