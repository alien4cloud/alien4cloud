package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Maps;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.matching.services.nodes.NodeMatcherService;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.CollectionUtils;

/**
 * This modifier load and put in context cache the matching candidates nodes.
 *
 * It does not update topology or matching configurations, these operations are
 * done in sub-sequent modifiers.
 */
@Component
public class NodeMatchingCandidateModifier extends AbstractMatchingCandidateModifier<LocationResourceTemplate> {
    @Inject
    private NodeMatcherService nodeMatcherService;

    @Inject
    private LocationService locationService;

    @Override
    protected String getResourceTemplateByTemplateIdCacheKey() {
        return FlowExecutionContext.SELECTED_MATCH_NODE_LOCATION_TEMPLATE_BY_NODE_ID_MAP;
    }

    @Override
    protected String getResourceTemplateByIdMapCacheKey() {
        return FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_ID_MAP;
    }

    @Override
    protected Map<String, List<LocationResourceTemplate>> getAvailableMatches(FlowExecutionContext context) {
        return (Map<String, List<LocationResourceTemplate>>) context.getExecutionCache()
                .get(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_NODE_ID_MAP);
    }

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(
                DeploymentMatchingConfiguration.class, NodeMatchingCandidateModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed
                                                  // first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();
        if (matchingConfiguration.getMatchedNodesConfiguration() == null) {
            matchingConfiguration.setMatchedNodesConfiguration(Maps.newHashMap());
        }
        Map<String, Set<String>> nodesPerLocation = (Map<String, Set<String>>) context.getExecutionCache().get(FlowExecutionContext.NODES_PER_LOCATIONS_CACHE_KEY);

        // TODO can we avoid update if the matching configuration is strickly younger
        // than the context last conf update ?
        // Fetch available substitutions on the selected locations.
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = computeAvailableSubstitutions(topology,
                nodesPerLocation, context.getEnvironmentContext().get().getEnvironment().getId());

        context.getExecutionCache().put(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_NODE_ID_MAP,
                availableSubstitutions);
        super.process(topology, context);
    }

    private Map<String, List<LocationResourceTemplate>> computeAvailableSubstitutions(Topology topology,
    Map<String, Set<String>> nodesPerLocation, String environmentId) {
        // Fetch all node types for templates in the topology
        Map<String, NodeType> nodeTypes = getNodeTypes(topology);
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = Maps.newHashMap();

        // Based on our model nodes may come from various locations actually.
        for (final Entry<String, Set<String>> nodesPerLocationEntry : nodesPerLocation.entrySet()) {
            Map<String, NodeTemplate> nodesToMatch = topology.getNodeTemplates().entrySet().stream()
                    .filter(e -> nodesPerLocationEntry.getValue().contains(e.getKey()))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            nodeMatcherService
                    .match(nodeTypes, nodesToMatch, locationService.getOrFail(nodesPerLocationEntry.getKey()),
                            environmentId)
                    .forEach((k, v) -> availableSubstitutions.merge(k, v, CollectionUtils::merge));
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
