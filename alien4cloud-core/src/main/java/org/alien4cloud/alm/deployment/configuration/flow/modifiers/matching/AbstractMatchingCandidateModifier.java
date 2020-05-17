package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;

/**
 * This matching modifier is responsible for automatic selection of location resources that are not yet selected by the user.
 *
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
public abstract class AbstractMatchingCandidateModifier<T extends AbstractLocationResourceTemplate> implements ITopologyModifier {

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                this.getClass().getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        Map<String, List<T>> availableMatches = getAvailableMatches(context);

        // Last user substitution may be incomplete or not valid anymore so let's check them and eventually select default values
        Map<String, T> availableResourceTemplatesById = Maps.newHashMap();
        Map<String, Set<String>> resourceTemplatesByTemplateId = Maps.newHashMap(); // map of nodeId -> location resource template ids required to create

        // historical deployment topology dto object
        for (Map.Entry<String, List<T>> entry : availableMatches.entrySet()) {
            // Fill locResTemplateIdsPerNodeIds
            Set<String> lrtIds = Sets.newHashSet();
            resourceTemplatesByTemplateId.put(entry.getKey(), lrtIds);
            // We leverage the loop to also create a map of resources by id for later usage.
            for (T lrt : entry.getValue()) {
                availableResourceTemplatesById.put(lrt.getId(), lrt);
                lrtIds.add(lrt.getId());
            }
        }

        // This is required for next modifier so we cache them here for performance reasons.
        context.getExecutionCache().put(getResourceTemplateByIdMapCacheKey(), availableResourceTemplatesById);
        context.getExecutionCache().put(getResourceTemplateByTemplateIdCacheKey(), resourceTemplatesByTemplateId);
    }


    protected abstract String getResourceTemplateByIdMapCacheKey();

    protected abstract String getResourceTemplateByTemplateIdCacheKey();

    protected abstract Map<String, List<T>> getAvailableMatches(FlowExecutionContext context);

}