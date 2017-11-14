package org.alien4cloud.tosca.editor.processors.groups;

import java.util.List;
import java.util.Map;
import java.util.Set;

import alien4cloud.exception.InvalidNameException;
import org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.*;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.exception.AlreadyExistException;
import org.alien4cloud.tosca.utils.TopologyUtils;

/**
 * Process the addition to a node template to a group. If the group does not exists, it is created.
 */
@Component
public class AddGroupMemberProcessor extends AbstractNodeProcessor<AddGroupMemberOperation> {
    @Override
    protected void processNodeOperation(Csar csar, Topology topology, AddGroupMemberOperation operation, NodeTemplate nodeTemplate) {
        // Ensure that the group exist and create it if not.
        Map<String, NodeGroup> groups = topology.getGroups();
        if (groups == null) {
            groups = Maps.newHashMap();
            topology.setGroups(groups);
        }
        NodeGroup nodeGroup = groups.get(operation.getGroupName());
        if (nodeGroup == null) {
            // If we create the group then check that the name is valid
            if (!operation.getGroupName().matches("\\w+")) {
                throw new InvalidNameException("groupName", operation.getGroupName(), "\\w+");
            }

            nodeGroup = new NodeGroup();
            nodeGroup.setName(operation.getGroupName());
            nodeGroup.setIndex(TopologyUtils.getAvailableGroupIndex(topology));
            Set<String> members = Sets.newHashSet();
            nodeGroup.setMembers(members);
            List<AbstractPolicy> policies = Lists.newArrayList();
            // For the moment, groups are created only for HA
            AbstractPolicy policy = new HaPolicy();
            policy.setName("High Availability");
            policies.add(policy);
            nodeGroup.setPolicies(policies);
            groups.put(operation.getGroupName(), nodeGroup);
        }

        // Add the node to the group and mark the node as member of the group.
        if (nodeTemplate.getGroups() == null) {
            nodeTemplate.setGroups(Sets.<String> newHashSet());
        }

        if (nodeGroup.getMembers().contains(operation.getNodeName())) {
            throw new AlreadyExistException("Node <" + operation.getNodeName() + "> is already member of group <" + operation.getGroupName() + ">.");
        }

        nodeTemplate.getGroups().add(operation.getGroupName());
        nodeGroup.getMembers().add(operation.getNodeName());
    }
}