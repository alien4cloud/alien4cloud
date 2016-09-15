package org.alien4cloud.tosca.catalog.index;

import org.alien4cloud.tosca.model.templates.Topology;

/**
 * Service responsible for indexing and updating topologies.
 */
public class TopologyCatalogService extends AbstractToscaIndexSearchService<Topology> {

    @Override
    protected Topology[] getArray(int size) {
        return new Topology[size];
    }
}
