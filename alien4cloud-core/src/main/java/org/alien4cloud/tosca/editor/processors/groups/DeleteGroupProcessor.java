package org.alien4cloud.tosca.editor.processors.groups;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.groups.DeleteGroupOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;

import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;

/**
 * Delete a group from a topology.
 */
public class DeleteGroupProcessor implements IEditorOperationProcessor<DeleteGroupOperation> {
    @Override
    public void process(DeleteGroupOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        NodeGroup nodeGroup = topology.getGroups().remove(operation.getGroupName());
        if (nodeGroup != null) {
            Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
            for (NodeTemplate nodeTemplate : nodeTemplates.values()) {
                if (nodeTemplate.getGroups() != null) {
                    nodeTemplate.getGroups().remove(operation.getGroupName());
                }
            }
        }
    }
}