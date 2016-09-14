package org.alien4cloud.tosca.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IncompatiblePropertyDefinitionException;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.topology.TopologyService;

/**
 * Helper service for editor context that allows to get input candidates based on a given topology.
 */
@Service
public class EditorInputHelperService {
    @Inject
    private TopologyService topologyService;
    @Inject
    private EditionContextManager editionContextManager;

    /**
     * Utility method to get the list of inputs (ids) that are compatible with the given property definition (no constraint conflicts)..
     *
     * @param topologyId The id of the topology for which to find input candidates.
     * @param nodeTemplateName The name of the node template for which to get input candidates.
     * @param propertyDefinitionGetter Implementation on how to get the property definition (from node properties, capabilities properties, relationships
     *            properties).
     * @return A list of input candidates that are compatible with the requested property definition.
     */
    public List<String> getInputCandidates(String topologyId, String nodeTemplateName, IPropertyDefinitionGetter propertyDefinitionGetter) {
        try {
            editionContextManager.init(topologyId);
            // check authorization to update a topology
            topologyService.checkEditionAuthorizations(EditionContextManager.getTopology());
            Topology topology = EditionContextManager.getTopology();
            NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
            PropertyDefinition pd = propertyDefinitionGetter.get(nodeTemplate);
            if (pd == null) {
                throw new NotFoundException("Unexpected error, property definition cannot be found for node <" + nodeTemplateName + ">");
            }

            Map<String, PropertyDefinition> inputs = topology.getInputs();
            List<String> inputIds = new ArrayList<String>();
            if (inputs != null && !inputs.isEmpty()) {
                // iterate overs existing inputs and filter them by checking constraint compatibility
                for (Map.Entry<String, PropertyDefinition> inputEntry : inputs.entrySet()) {
                    try {
                        inputEntry.getValue().checkIfCompatibleOrFail(pd);
                        inputIds.add(inputEntry.getKey());
                    } catch (IncompatiblePropertyDefinitionException e) {
                        // Nothing to do here, the id won't be added to the list
                    }
                }
            }
            return inputIds;
        } finally {
            editionContextManager.destroy();
        }
    }

    /**
     * Interface to implement to get a property definition from a node template. Implementations can retrieve it from the node, it's capabilites or relations.
     */
    public interface IPropertyDefinitionGetter {
        /**
         * Retrieve a property definition from a node type. Note that a ToscaContext will and must be opened before the get operation is called.
         *
         * @param nodeTemplate The node template from which to get the property definition.
         * @return A property definition of null if no property definition can be found on the node type.
         */
        PropertyDefinition get(NodeTemplate nodeTemplate);
    }
}
