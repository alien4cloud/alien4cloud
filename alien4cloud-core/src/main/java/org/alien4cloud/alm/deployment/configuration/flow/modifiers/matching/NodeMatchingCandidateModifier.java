package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.deployment.matching.services.nodes.NodeMatcherService;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.AlienConstants;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * This modifier load and put in context cache the matching candidates nodes.
 *
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
@Component
public class NodeMatchingCandidateModifier implements ITopologyModifier {
    @Inject
    private NodeMatcherService nodeMatcherService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingCandidateModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();
        if (matchingConfiguration.getMatchedNodesConfiguration() == null) {
            matchingConfiguration.setMatchedNodesConfiguration(Maps.newHashMap());
        }
        if (matchingConfiguration.getMatchedLocationResources() == null) {
            matchingConfiguration.setMatchedLocationResources(Maps.newHashMap());
        }

        Map<String, Location> locationMap = (Map<String, Location>) context.getExecutionCache().get(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY);
        // TODO can we avoid update if the matching configuration is strickly younger than the context last conf update ?
        // Fetch available substitutions on the selected locations.
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = getAvailableSubstitutions(topology, matchingConfiguration.getLocationGroups(),
                locationMap, context.getEnvironmentContext().get().getEnvironment().getId());

        context.getExecutionCache().put(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_NODE_ID_MAP, availableSubstitutions);
    }

    private Map<String, List<LocationResourceTemplate>> getAvailableSubstitutions(Topology topology, Map<String, NodeGroup> locationGroups,
            Map<String, Location> locationByIds, String environmentId) {
        // Fetch all node types for templates in the topology
        Map<String, NodeType> nodeTypes = getNodeTypes(topology);
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = Maps.newHashMap();
        // Based on our model nodes may come from various locations actually.
        for (final Map.Entry<String, NodeGroup> locationGroupEntry : locationGroups.entrySet()) {
            String groupName = locationGroupEntry.getKey();
            final NodeGroup locationNodeGroup = locationGroupEntry.getValue();
            Map<String, NodeTemplate> nodesToMatch = Maps.newHashMap();
            if (MapUtils.isNotEmpty(topology.getNodeTemplates())) {
                if (AlienConstants.GROUP_ALL.equals(groupName)) {
                    locationNodeGroup.setMembers(topology.getNodeTemplates().keySet());
                    nodesToMatch = topology.getNodeTemplates();
                } else {
                    nodesToMatch = Maps.filterEntries(topology.getNodeTemplates(), input -> locationNodeGroup.getMembers().contains(input.getKey()));
                }
            }
            availableSubstitutions.putAll(nodeMatcherService.match(nodeTypes, nodesToMatch, locationByIds.get(groupName), environmentId));
        }
        return availableSubstitutions;
    }

    private Map<String, NodeType> getNodeTypes(Topology topology) {
        Map<String, NodeType> nodeTypes = Maps.newHashMap();
        for (NodeTemplate template : safe(topology.getNodeTemplates()).values()) {
            if (!nodeTypes.containsKey(template.getType())) {
                NodeType nodeType = ToscaContext.getOrFail(NodeType.class, template.getType());
                nodeTypes.put(nodeType.getElementId(), nodeType);
            }
        }
        return nodeTypes;
    }
}
