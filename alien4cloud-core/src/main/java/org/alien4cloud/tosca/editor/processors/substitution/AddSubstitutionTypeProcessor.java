package org.alien4cloud.tosca.editor.processors.substitution;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.templates.SubstitutionMapping;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.topology.TopologyService;

/**
 * Process the creation of topology template as substitute.
 */
@Component
public class AddSubstitutionTypeProcessor implements IEditorOperationProcessor<AddSubstitutionTypeOperation> {
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private TopologyService topologyService;

    @Override
    public void process(AddSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        if (topology.getSubstitutionMapping() == null) {
            topology.setSubstitutionMapping(new SubstitutionMapping());
        }

        NodeType nodeType = toscaTypeSearchService.getElementInDependencies(NodeType.class, operation.getElementId(), topology.getDependencies());
        // if not null the node type exists in the dependencies, there is no choices for this type version
        if (nodeType == null) {
            // the node type does'nt exist in this topology dependencies
            // we need to find the latest version of this component and use it as default
            nodeType = toscaTypeSearchService.findMostRecent(NodeType.class, operation.getElementId());
            topologyService.loadType(topology, nodeType);
        }
        topology.getSubstitutionMapping().setSubstitutionType(operation.getElementId());
    }
}