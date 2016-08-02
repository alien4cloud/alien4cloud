package org.alien4cloud.tosca.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.topology.TopologyServiceCore;
import org.springframework.stereotype.Service;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IncompatiblePropertyDefinitionException;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyService;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Helper service for editor context that allows to get possible replacement indexedNodeTypes for a node template.
 */
@Service
public class EditorNodeReplacementService {
    @Inject
    private TopologyService topologyService;
    @Inject
    private EditionContextManager editionContextManager;

    /**
     * Utility method to get possible replacement indexedNodeTypes for a node template
     *
     * @param topologyId The id of the topology for which to find possible nodes replacement.
     * @param nodeTemplateName The name of the node template for which to get possible nodes replacement.
     * @return An array of possible replacement for a node template.
     */
    public IndexedNodeType[] getReplacementForNode(String topologyId, String nodeTemplateName) {
        try {
            editionContextManager.init(topologyId);
            // check authorization to update a topology
            topologyService.checkEditionAuthorizations(EditionContextManager.getTopology());
            Topology topology = EditionContextManager.getTopology();

            TopologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, TopologyServiceCore.getNodeTemplates(topology));
            return topologyService.findReplacementForNode(nodeTemplateName, topology);
        } finally {
            editionContextManager.destroy();
        }
    }
}
