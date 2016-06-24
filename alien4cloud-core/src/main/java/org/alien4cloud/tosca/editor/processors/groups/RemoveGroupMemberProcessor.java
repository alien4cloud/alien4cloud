package org.alien4cloud.tosca.editor.processors.groups;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation;
import org.alien4cloud.tosca.editor.processors.AbstractNodeProcessor;

import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;

/**
 * Remove a node from a group.
 */
public class RemoveGroupMemberProcessor extends AbstractNodeProcessor<RemoveGroupMemberOperation> {
    @Override
    protected void processNodeOperation(RemoveGroupMemberOperation operation, NodeTemplate nodeTemplate) {
        Topology topology = TopologyEditionContextManager.getTopology();

        NodeGroup nodeGroup = topology.getGroups().get(operation.getGroupName());
        if (nodeGroup != null && nodeGroup.getMembers() != null) {
            nodeGroup.getMembers().remove(operation.getNodeName());
        }

        if (nodeTemplate != null && nodeTemplate.getGroups() != null) {
            nodeTemplate.getGroups().remove(operation.getGroupName());
        }
    }
}