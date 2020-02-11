package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.DeploymentTopologyDTOBuilder;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.normative.NormativeNetworkConstants;

/**
 * ComputeMatchingElementsDependenciesModifier
 */
@Component
public class ComputeLocationGroupsModifier implements ITopologyModifier {

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        // TODO(loicalbertin) do we need this?
        // Map<String, Set<String>> policiesDeps = findPoliciesDependencies(topology);

        Map<String, Set<String>> nodesGroups = findNodesGroups(topology);

        Optional<DeploymentMatchingConfiguration> matchingConfigurationOptional = context.getConfiguration(
                DeploymentMatchingConfiguration.class, DeploymentTopologyDTOBuilder.class.getSimpleName());

        DeploymentMatchingConfiguration configuration = matchingConfigurationOptional
                .orElseGet(() -> newMatchingConfiguration(context));

        Map<String, NodeGroup> groups = safe(configuration.getLocationGroups());
        // Remove groups that do not exists anymore
        groups.entrySet().removeIf(e -> !nodesGroups.containsKey(e.getValue().getMembers().iterator().next()));


        for (Entry<String, Set<String>> entry : nodesGroups.entrySet()) {
            String groupName = entry.getKey() + "_Group";
            NodeGroup group = groups.computeIfAbsent(groupName, k -> new NodeGroup());
            // Always reset name & members
            group.setName(groupName);
            group.setMembers(new LinkedHashSet<>());
            // Set master node (generally the compute) as first member
            group.getMembers().add(entry.getKey());
            group.getMembers().addAll(entry.getValue());
        }

        configuration.setLocationGroups(groups);

        context.saveConfiguration(configuration);

    }

    private Map<String, Set<String>> findPoliciesDependencies(Topology topology) {
        return safe(topology.getPolicies()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, this::getPolicyTarget));
    }

    private Set<String> getPolicyTarget(Map.Entry<String, PolicyTemplate> policyEntry) {
        return policyEntry.getValue().getTargets();
    }

    private Map<String, Set<String>> findNodesGroups(Topology topology) {
        Map<String, Set<String>> results = Maps.newHashMap();
        Collection<NodeTemplate> abstractNodes = safe(topology.getNodeTemplates()).values();
        // At first consider each node
        abstractNodes.forEach(n -> results.put(n.getName(), Sets.newHashSet()));
        abstractNodes.forEach(node1 -> {
            abstractNodes.forEach(node2 -> {
                if (node1 != node2) {
                    if (isHostedOn(topology, node1, node2) || isAttachToBlockStorage(topology, node1, node2)
                            || isConnectsToNetwork(topology, node2, node1)) {
                        results.computeIfAbsent(node2.getName(), k -> Sets.newHashSet()).add(node1.getName());
                        // node1 is part of the node2's group remove it
                        results.remove(node1.getName());
                    }
                }
            });
        });
        return results;
    }

    private boolean isHostedOn(Topology topology, NodeTemplate source, NodeTemplate target) {
        NodeTemplate host = TopologyNavigationUtil.getImmediateHostTemplate(topology, source);
        if (host == null) {
            return false;
        }
        if (host == target) {
            return true;
        }
        return isHostedOn(topology, host, target);
    }

    private boolean isAttachToBlockStorage(Topology topology, NodeTemplate source, NodeTemplate target) {
        NodeType targetType = ToscaContext.getOrFail(NodeType.class, target.getType());
        if (!ToscaTypeUtils.isOfType(targetType, NormativeComputeConstants.COMPUTE_TYPE)) {
            return false;
        }
        return safe(source.getRelationships()).values().stream().anyMatch(r -> {
            return ToscaTypeUtils.isOfType(ToscaContext.getOrFail(RelationshipType.class, r.getType()),
                    NormativeRelationshipConstants.ATTACH_TO) && r.getTarget().equals(target.getName());
        });

    }

    private boolean isConnectsToNetwork(Topology topology, NodeTemplate source, NodeTemplate target) {
        NodeType targetType = ToscaContext.getOrFail(NodeType.class, target.getType());
        if (!ToscaTypeUtils.isOfType(targetType, NormativeNetworkConstants.NETWORK_TYPE)) {
            return false;
        }
        return safe(source.getRelationships()).values().stream().anyMatch(r -> {
            return ToscaTypeUtils.isOfType(ToscaContext.getOrFail(RelationshipType.class, r.getType()),
                    NormativeRelationshipConstants.NETWORK) && r.getTarget().equals(target.getName());
        });
    }

    private DeploymentMatchingConfiguration newMatchingConfiguration(FlowExecutionContext context) {
        ApplicationEnvironment environment = context.getEnvironmentContext()
                .orElseThrow(() -> new IllegalArgumentException("Input modifier requires an environment context."))
                .getEnvironment();
        DeploymentMatchingConfiguration configuration = new DeploymentMatchingConfiguration(
                environment.getTopologyVersion(), environment.getId());
        configuration.setLocationGroups(new HashMap<>());
        return configuration;
    }
}