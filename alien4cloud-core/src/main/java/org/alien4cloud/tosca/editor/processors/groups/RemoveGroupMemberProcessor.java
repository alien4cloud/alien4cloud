package org.alien4cloud.tosca.editor.processors.groups;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

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
            boolean removed = nodeGroup.getMembers().remove(operation.getNodeName());
            if (!removed) {
                throw new NotFoundException("Node <" + operation.getNodeName() + "> is not part of group <" + operation.getGroupName() + ">.");
            }
        }

        if (nodeTemplate != null && nodeTemplate.getGroups() != null) {
            nodeTemplate.getGroups().remove(operation.getGroupName());
        }
    }
}