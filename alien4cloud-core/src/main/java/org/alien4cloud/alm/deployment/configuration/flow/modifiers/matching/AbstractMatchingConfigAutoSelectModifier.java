package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.topology.task.NodeMatchingTask;

/**
 * This matching modifier is responsible for automatic selection of location resources that are not yet selected by the user.
 *
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
public abstract class AbstractMatchingConfigAutoSelectModifier<T extends AbstractLocationResourceTemplate> implements ITopologyModifier {

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                this.getClass().getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        Map<String, String> lastUserMatches = getLastUserMatches(matchingConfiguration);
        Map<String, List<T>> availableMatches = getAvailableMatches(context);

        // Last user substitution may be incomplete or not valid anymore so let's check them and eventually select default values
        // historical deployment topology dto object
        for (Map.Entry<String, List<T>> entry : availableMatches.entrySet()) {
            // select default values if not set but also reconsider auto-matchings
            if (!lastUserMatches.containsKey(entry.getKey())) {
                if (entry.getValue().isEmpty()) {
                    // report that no node has been found on the location with the topology criteria
                    consumeNoMatchingFound(context, new NodeMatchingTask(entry.getKey()));
                } else {
                    performAutomaticMatching(context, matchingConfiguration, lastUserMatches, entry.getKey(), entry.getValue());
                }
            }
        }

        // matchingConfiguration.setMatchedPolicies(lastUserMatches);
        // TODO Do that only if updated...
        context.saveConfiguration(matchingConfiguration);
    }

    private void performAutomaticMatching(FlowExecutionContext context,
            DeploymentMatchingConfiguration matchingConfiguration, Map<String, String> lastUserMatches,
            String templateName, List<T> templateMatches) {
        // by default chose a random one
        String selectedLocationTemplateId = templateMatches.iterator().next().getId();

        // Only take the first element as selected if no configuration has been set before
        // let an info so the user know that we made a default selection for him
        context.log().info("Automatic matching for template <" + templateName + ">");
        lastUserMatches.put(templateName, selectedLocationTemplateId);
    }


    protected abstract Map<String, String> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration);

    protected abstract Map<String, List<T>> getAvailableMatches(FlowExecutionContext context);

    protected abstract void consumeNoMatchingFound(FlowExecutionContext context, AbstractTask task);

}