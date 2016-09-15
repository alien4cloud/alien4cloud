package org.alien4cloud.tosca.editor.processors.substitution;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.templates.SubstitutionMapping;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.catalog.index.ToscaTypeSearchService;
import alien4cloud.topology.TopologyService;

/**
 * Process the creation of topology template as substitute.
 */
@Component
public class AddSubstitutionTypeProcessor implements IEditorOperationProcessor<AddSubstitutionTypeOperation> {

    @Resource
    private ToscaTypeSearchService csarRepoSearchService;
    @Inject
    private TopologyService topologyService;

    @Override
    public void process(AddSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        // FIXME we don't allow substitution for applications YET (this has to be changed as Application could become a service in the future and this would be
        // done through substitution)
        // if (!topology.getDelegateType().equals(TopologyTemplate.class.getSimpleName().toLowerCase())) {
        // throw new InvalidArgumentException("This operation is only allowed for topology templates");
        // }

        if (topology.getSubstitutionMapping() == null) {
            topology.setSubstitutionMapping(new SubstitutionMapping());
        }

        NodeType nodeType = csarRepoSearchService.getElementInDependencies(NodeType.class, operation.getElementId(), topology.getDependencies());
        // if not null the node type exists in the dependencies, there is no choices for this type version
        if (nodeType == null) {
            // the node type does'nt exist in this topology dependencies
            // we need to find the latest version of this component and use it as default
            nodeType = csarRepoSearchService.findMostRecent(NodeType.class, operation.getElementId());
            // FIXME we should use type loader here to avoid conflicts
            topology.getDependencies().add(topologyService.buildDependencyBean(nodeType.getArchiveName(), nodeType.getArchiveVersion()));
        }
        topology.getSubstitutionMapping().setSubstitutionType(nodeType);
    }
}