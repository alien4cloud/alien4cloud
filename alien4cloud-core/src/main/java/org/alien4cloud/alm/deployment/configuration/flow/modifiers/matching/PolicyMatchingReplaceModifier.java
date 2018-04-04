package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import alien4cloud.model.deployment.matching.ILocationMatch;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PluginModifierRegistry;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.plugin.exception.MissingPluginException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.TagUtil;

/**
 * Last policy matching modifier, it actually inject policy modifiers implementations as policies implementations may impact the actual topology to be deployed.
 */
@Component
public class PolicyMatchingReplaceModifier extends AbstractMatchingReplaceModifier<PolicyTemplate, PolicyLocationResourceTemplate, PolicyType> {
    @Inject
    private PluginModifierRegistry pluginModifierRegistry;

    /**
     * Add locations dependencies
     */
    @Override
    protected void init(Topology topology, FlowExecutionContext context) {
        List<ILocationMatch> locations = (List<ILocationMatch>) context.getExecutionCache().get(FlowExecutionContext.LOCATION_MATCH_CACHE_KEY);
        for (ILocationMatch location : locations) {
            // FIXME manage conflicting dependencies by fetching types from latest version
            topology.getDependencies().addAll(location.getLocation().getDependencies());
        }
        ToscaContext.get().resetDependencies(topology.getDependencies());
    }

    @Override
    protected Class<PolicyType> getToscaTypeClass() {
        return PolicyType.class;
    }

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        super.process(topology, context);

        // In addition to processing policy template replacements we also inject implementation modifiers for policies that defines them

        for (PolicyTemplate policyTemplate : safe(topology.getPolicies()).values()) {
            PolicyType policyType = ToscaContext.getOrFail(PolicyType.class, policyTemplate.getType());
            String policyImplMeta = TagUtil.getTagValue(policyType.getTags(), "a4c_policy_impl");
            if (policyImplMeta == null) {
                context.log().warn("Matched policy {} for {} does not define an alien topology modifier implementation, it may not be taken in account.",
                        policyTemplate.getType(), policyTemplate.getName());
                continue;
            }
            String[] policyImpl = policyImplMeta.split(":");
            if (policyImpl.length != 3) {
                context.log().error(
                        "Matched policy {} for policy {} defines an invalid modifier implementation {}, format should be policy_plugin_id:policy_plugin_bean:injection_phase",
                        policyTemplate.getType(), policyTemplate.getName(), policyImplMeta);
            }

            try {
                ITopologyModifier modifier = pluginModifierRegistry.getPluginBean(policyImpl[0], policyImpl[1]);
                List<ITopologyModifier> phaseModifiers = (List<ITopologyModifier>) context.getExecutionCache().computeIfAbsent(policyImpl[2],
                        s -> Lists.<ITopologyModifier> newArrayList());
                // No need to add a modifier more than once for a phase
                if (!phaseModifiers.contains(modifier)) {
                    phaseModifiers.add(modifier);
                }
            } catch (MissingPluginException e) {
                context.log().error("Implementation specified for policy type {} that refers to plugin bean {}, {} cannot be found.", policyTemplate.getType(),
                        policyImpl[0], policyImpl[1]);
            }
        }
    }

    @Override
    protected String getOriginalTemplateCacheKey() {
        return FlowExecutionContext.MATCHING_ORIGINAL_POLICIES;
    }

    @Override
    protected String getReplacedTemplateCacheKey() {
        return FlowExecutionContext.MATCHING_REPLACED_POLICIES;
    }

    @Override
    protected Map<String, PolicyLocationResourceTemplate> getMatchesById(FlowExecutionContext context) {
        return (Map<String, PolicyLocationResourceTemplate>) context.getExecutionCache().get(FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_ID_MAP);
    }

    @Override
    protected Map<String, String> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedPolicies();
    }

    @Override
    protected Map<String, PolicyTemplate> getTopologyTemplates(Topology topology) {
        return topology.getPolicies();
    }

    @Override
    protected PolicyLocationResourceTemplate getLocationResourceTemplateCopy(String locationResourceTemplateId) {
        return getLocationResourceService().getOrFail(locationResourceTemplateId);
    }

    @Override
    protected void processSpecificReplacement(PolicyTemplate replacingNode, PolicyTemplate replacedTopologyNode, Set<String> topologyNotMergedProps) {
        replacingNode.setTargets(replacedTopologyNode.getTargets());
        replacingNode.setTriggers(replacedTopologyNode.getTriggers());
    }
}