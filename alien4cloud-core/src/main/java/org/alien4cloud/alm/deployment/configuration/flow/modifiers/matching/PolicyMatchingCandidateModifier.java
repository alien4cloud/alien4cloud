package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.collect.Maps;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.matching.services.policies.PolicyMatcherService;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;
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

    private Map<String, List<PolicyLocationResourceTemplate>> computeAvailableMatches(Topology topology, FlowExecutionContext context,
            Map<String, NodeGroup> locationGroups, Map<String, Location> locationByIds, String environmentId) {
        Map<String, List<PolicyLocationResourceTemplate>> availableSubstitutions = Maps.newHashMap();
        // Fetch all policy types for templates in the topology
        Map<String, PolicyType> policyTypes = getPolicyTypes(topology);
        for (Map.Entry<String, NodeGroup> entry: locationGroups.entrySet()) {

                policyMatcherService.match(topology.getPolicies(), policyTypes, locationByIds.get(entry.getKey()), environmentId).forEach( (k,v)->{
                    availableSubstitutions.merge(k, v, CollectionUtils::merge);
                });
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
