package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
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

import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.normative.NormativeNetworkConstants;

/**
 * ComputeMatchingElementsDependenciesModifier
 */
@Component
public class ComputeMatchingElementsDependenciesModifier implements ITopologyModifier {

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context
                .getConfiguration(DeploymentMatchingConfiguration.class, this.getClass().getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed
                                                  // first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();
        Map<String, Set<String>> policiesDeps = findPoliciesDependencies(topology);

        Map<String, Set<String>> nodesDeps =findAbstractNodesDependencies(topology);

        matchingConfiguration.setRelatedMatchedEntities(Stream.concat(policiesDeps.entrySet().stream(), nodesDeps.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        context.saveConfiguration(matchingConfiguration);
    }

    private Map<String, Set<String>> findPoliciesDependencies(Topology topology) {
        return safe(topology.getPolicies()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, this::getPolicyTarget));
    }

    private Set<String> getPolicyTarget(Map.Entry<String, PolicyTemplate> policyEntry) {
        return policyEntry.getValue().getTargets();
    }

    private Map<String, Set<String>> findAbstractNodesDependencies(Topology topology) {
        Map<String, Set<String>> results = Maps.newHashMap();
        Set<NodeTemplate> abstractNodes = safe(topology.getNodeTemplates()).values().stream().filter(n -> {
            NodeType nodeType = ToscaContext.getOrFail(NodeType.class, n.getType());
            return nodeType.isAbstract();
        }).collect(Collectors.toSet());

        abstractNodes.forEach(source -> {
            abstractNodes.forEach(target -> {
                if (source != target) {
                    if (isHostedOn(topology, source, target) || isAttachToBlockStorage(topology, source, target)
                            || isConnectsToNetwork(topology, source, target)) {
                        results.computeIfAbsent(source.getName(), k -> Sets.newHashSet()).add(target.getName());
                        results.computeIfAbsent(target.getName(), k -> Sets.newHashSet()).add(source.getName());
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
}