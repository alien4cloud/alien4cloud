package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.deployment.matching.services.policies.PolicyMatcherService;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.AlienConstants;

/**
 * This modifier load and put in context cache the matching candidates nodes.
 *
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
@Component
public class PolicyMatchingCandidateModifier implements ITopologyModifier {
    @Inject
    private PolicyMatcherService policyMatcherService;

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
        Map<String, List<PolicyLocationResourceTemplate>> availableSubstitutions = getAvailableMatches(topology, context,
                matchingConfiguration.getLocationGroups(), locationMap, context.getEnvironmentContext().get().getEnvironment().getId());

        context.getExecutionCache().put(FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_NODE_ID_MAP, availableSubstitutions);
    }

    private Map<String, List<PolicyLocationResourceTemplate>> getAvailableMatches(Topology topology, FlowExecutionContext context,
            Map<String, NodeGroup> locationGroups, Map<String, Location> locationByIds, String environmentId) {
        Map<String, List<PolicyLocationResourceTemplate>> availableSubstitutions = Maps.newHashMap();
        if (locationGroups.size() > 1) {
            // Fail as not yet supported in alien4cloud.
            // Supporting policy management on multiple locations could have limitations based on the policy (need to be defined on both locations with
            // identical requirements / access to services etc.)
            // Unless defined on nodes that are all in the same location.
            context.log().warn("Policy are not supported when deployment is performed on multiple locations.");
            return availableSubstitutions;
        }

        // Fetch all policy types for templates in the topology
        Map<String, PolicyType> policyTypes = getPolicyTypes(topology);
        availableSubstitutions
                .putAll(policyMatcherService.match(topology.getPolicies(), policyTypes, locationByIds.get(AlienConstants.GROUP_ALL), environmentId));

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
