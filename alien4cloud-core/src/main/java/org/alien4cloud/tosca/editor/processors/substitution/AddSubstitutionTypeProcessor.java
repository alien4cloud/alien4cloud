package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.CSARDependency;
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
    private IToscaTypeSearchService csarRepoSearchService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private ICsarService csarService;

    @Override
    public void process(AddSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        // FIXME we don't allow substitution for applications YET (this has to be changed as Application could become a service in the future and this would be
        // done through substitution)
        if (Objects.equals(EditionContextManager.getCsar().getDelegateType(), ArchiveDelegateType.APPLICATION)) {
            throw new UnsupportedOperationException("Add substitution type operation is only allowed for topology templates");
        }

        if (topology.getSubstitutionMapping() == null) {
            topology.setSubstitutionMapping(new SubstitutionMapping());
        }

        NodeType nodeType = csarRepoSearchService.getElementInDependencies(NodeType.class, operation.getElementId(), topology.getDependencies());
        // if not null the node type exists in the dependencies, there is no choices for this type version
        if (nodeType == null) {
            // the node type does'nt exist in this topology dependencies
            // we need to find the latest version of this component and use it as default
            nodeType = csarRepoSearchService.findMostRecent(NodeType.class, operation.getElementId());
            Set<CSARDependency> oldDependencies = topology.getDependencies();
            topologyService.loadType(topology, nodeType);
            if (!Objects.equals(topology.getDependencies(), oldDependencies)) {
                csarService.setDependencies(topology.getId(), topology.getDependencies());
            }
        }
        topology.getSubstitutionMapping().setSubstitutionType(nodeType);
    }
}