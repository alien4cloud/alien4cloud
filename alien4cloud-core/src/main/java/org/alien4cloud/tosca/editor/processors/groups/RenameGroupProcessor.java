package org.alien4cloud.tosca.editor.processors.groups;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;

/**
 * Rename a group in the topology under edition.
 */
public class RenameGroupProcessor implements IEditorOperationProcessor<RenameGroupOperation> {
    @Override
    public void process(RenameGroupOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        if (operation.getGroupName().equals(operation.getNewGroupName())) {
            return; // nothing has changed.
        }

        if (topology.getGroups().containsKey(operation.getGroupName())) {
            throw new AlreadyExistException("Group with name [" + operation.getGroupName() + "] already exists, please choose another name");
        }

        NodeGroup nodeGroup = topology.getGroups().remove(operation.getGroupName());
        if (nodeGroup != null) {
            nodeGroup.setName(operation.getNewGroupName());
            Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
            for (NodeTemplate nodeTemplate : nodeTemplates.values()) {
                if (nodeTemplate.getGroups() != null) {
                    if (nodeTemplate.getGroups().remove(operation.getGroupName())) {
                        nodeTemplate.getGroups().add(operation.getNewGroupName());
                    }
                }
            }
            topology.getGroups().put(operation.getNewGroupName(), nodeGroup);
        }
    }
}
