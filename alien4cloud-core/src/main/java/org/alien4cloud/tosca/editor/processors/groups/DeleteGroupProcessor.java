package org.alien4cloud.tosca.editor.processors.groups;

import static alien4cloud.utils.AlienUtils.safe;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.groups.DeleteGroupOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

/**
 * Delete a group from a topology.
 */
@Component
public class DeleteGroupProcessor implements IEditorOperationProcessor<DeleteGroupOperation> {
    @Override
    public void process(DeleteGroupOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        NodeGroup nodeGroup = topology.getGroups() == null ? null : topology.getGroups().remove(operation.getGroupName());
        if (nodeGroup == null) {
            throw new NotFoundException("Group " + operation.getGroupName() + " does not exists");
        }
        for (NodeTemplate nodeTemplate : safe(topology.getNodeTemplates()).values()) {
            if (nodeTemplate.getGroups() != null) {
                nodeTemplate.getGroups().remove(operation.getGroupName());
            }
        }
    }
}