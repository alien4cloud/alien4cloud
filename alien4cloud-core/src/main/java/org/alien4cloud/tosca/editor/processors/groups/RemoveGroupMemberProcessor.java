package org.alien4cloud.tosca.editor.processors.groups;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;

/**
 * Remove a node from a group.
 */
@Component
public class RemoveGroupMemberProcessor extends AbstractNodeProcessor<RemoveGroupMemberOperation> {
    @Override
    protected void processNodeOperation(RemoveGroupMemberOperation operation, NodeTemplate nodeTemplate) {
        Topology topology = EditionContextManager.getTopology();

        NodeGroup nodeGroup = topology.getGroups().get(operation.getGroupName());
        if (nodeGroup != null && nodeGroup.getMembers() != null) {
            nodeGroup.getMembers().remove(operation.getNodeName());
        }

        if (nodeTemplate != null && nodeTemplate.getGroups() != null) {
            nodeTemplate.getGroups().remove(operation.getGroupName());
        }
    }
}