package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Maps;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.AbstractPolicy;
import org.alien4cloud.tosca.model.templates.LocationPlacementPolicy;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.matching.services.policies.PolicyMatcherService;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.CollectionUtils;

/**
 * This modifier load and put in context cache the matching candidates nodes.
 *
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
@Component
public class PolicyMatchingCandidateModifier extends AbstractMatchingCandidateModifier<PolicyLocationResourceTemplate> {
    @Inject
    private PolicyMatcherService policyMatcherService;

    @Inject
    private LocationService locationService;


    @Override
    protected String getResourceTemplateByTemplateIdCacheKey() {
        return FlowExecutionContext.SELECTED_MATCH_POLICY_LOCATION_TEMPLATE_BY_NODE_ID_MAP;
    }

    @Override
    protected String getResourceTemplateByIdMapCacheKey() {
        return FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_ID_MAP;
    }


    @Override
    protected Map<String, List<PolicyLocationResourceTemplate>> getAvailableMatches(FlowExecutionContext context) {
        return (Map<String, List<PolicyLocationResourceTemplate>>) context.getExecutionCache()
                .get(FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_NODE_ID_MAP);
    }


    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                PolicyMatchingCandidateModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();
        if (matchingConfiguration.getMatchedPolicies() == null) {
            matchingConfiguration.setMatchedPolicies(Maps.newHashMap());
        }

        Map<String, Location> locationMap = (Map<String, Location>) context.getExecutionCache().get(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY);

        // TODO avoid update if the matching configuration is strickly younger than the context last conf update ?
        // Fetch available substitutions on the selected locations.
        Map<String, List<PolicyLocationResourceTemplate>> availableSubstitutions = computeAvailableMatches(topology, context,
                matchingConfiguration.getLocationGroups(), locationMap, context.getEnvironmentContext().get().getEnvironment().getId());

        context.getExecutionCache().put(FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_NODE_ID_MAP, availableSubstitutions);

        super.process(topology, context);
    }

    private String getLocationForNode(FlowExecutionContext context, Map<String, NodeGroup> locationGroups, String nodeName) {
        for (NodeGroup group : locationGroups.values()) {
            if (group.getMembers().contains(nodeName)) {
                if (safe(group.getPolicies()).size() > 0 ) {
                    AbstractPolicy ap = group.getPolicies().iterator().next();
                    if (ap instanceof LocationPlacementPolicy) {
                        return ((LocationPlacementPolicy) ap).getLocationId();
                    }
                }
            }
        }
        String errMessage = "Node <"+nodeName+"> doesn't have a associated placement policy";
        context.log().error(errMessage, TaskCode.LOCATION_POLICY);
        throw new RuntimeException(errMessage);
    }

    private Map<String, List<PolicyLocationResourceTemplate>> computeAvailableMatches(Topology topology, FlowExecutionContext context,
            Map<String, NodeGroup> locationGroups, Map<String, Location> locationByIds, String environmentId) {
        Map<String, List<PolicyLocationResourceTemplate>> availableSubstitutions = Maps.newHashMap();
        // Fetch all policy types for templates in the topology
        Map<String, PolicyType> policyTypes = getPolicyTypes(topology);

        // Build a policies templates names by group location id map
        Map<String, Collection<String>> policiesByLocation = Maps.newHashMap();

        safe(topology.getPolicies()).values().forEach( p -> {
            String locationId = null;
            for (String nodeName : safe(p.getTargets())) {
                String nodeLocation = getLocationForNode(context, locationGroups, nodeName);
                if (locationId == null) {
                    locationId = nodeLocation;
                } else if (!locationId.equals(nodeLocation)) {
                    context.log().error(new LocationPolicyTask());
                    context.log().error("Policy <"+p.getName()+"> could not be affected to targets "+p.getTargets()+" which are not in the same location. Either use different Policies Templates or use the same location for all nodes.", TaskCode.LOCATION_POLICY);
                    return ;
                }
            }
            if (locationId != null) {
                policiesByLocation.computeIfAbsent(locationId, k-> new HashSet<String>()).add(p.getName());
            }
        });

        for (Map.Entry<String, Collection<String>> entry: policiesByLocation.entrySet()) {
                Map<String, PolicyTemplate> policies = topology.getPolicies().entrySet().stream().filter(e -> entry.getValue().contains(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                Map<String, List<PolicyLocationResourceTemplate>> matches =
                    policyMatcherService.match(policies, policyTypes, locationService.getOrFail(entry.getKey()), environmentId);
                matches.forEach( (k,v)->availableSubstitutions.merge(k, v, CollectionUtils::merge));
        }

        return availableSubstitutions;
    }

    private Map<String, PolicyType> getPolicyTypes(Topology topology) {
        Map<String, PolicyType> policyTypes = Maps.newHashMap();
        for (PolicyTemplate template : safe(topology.getPolicies()).values()) {
            if (!policyTypes.containsKey(template.getType())) {
                PolicyType policyType = ToscaContext.getOrFail(PolicyType.class, template.getType());
                policyTypes.put(policyType.getElementId(), policyType);
            }
        }
        return policyTypes;
    }
}
