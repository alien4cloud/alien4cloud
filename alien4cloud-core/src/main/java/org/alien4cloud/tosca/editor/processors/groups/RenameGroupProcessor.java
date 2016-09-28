package org.alien4cloud.tosca.editor.processors.groups;

import java.util.Map;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.topology.TopologyServiceCore;

/**
 * Rename a group in the topology under edition.
 */
@Component
public class RenameGroupProcessor implements IEditorOperationProcessor<RenameGroupOperation> {
    @Override
    public void process(RenameGroupOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        if (operation.getNewGroupName() == null || !operation.getNewGroupName().matches("\\w+")) {
            throw new InvalidNameException("groupName", operation.getGroupName(), "\\w+");
        }

        if (topology.getGroups() == null) {
            throw new NotFoundException("Group with name [" + operation.getGroupName() + "] does not exists and cannot be renamed.");
        }

        if (topology.getGroups().containsKey(operation.getNewGroupName())) {
            throw new AlreadyExistException("Group with name [" + operation.getNewGroupName() + "] already exists, please choose another name");
        }

        NodeGroup nodeGroup = topology.getGroups().remove(operation.getGroupName());
        if (nodeGroup == null) {
            throw new NotFoundException("Group with name [" + operation.getGroupName() + "] does not exists and cannot be renamed.");
        }
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
