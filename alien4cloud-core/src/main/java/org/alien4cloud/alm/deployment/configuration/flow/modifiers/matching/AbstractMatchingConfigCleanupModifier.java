package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;

/**
 * Base class for the Node and Policy MatchingConfigCleanupModifier.
 */
public abstract class AbstractMatchingConfigCleanupModifier<T extends AbstractLocationResourceTemplate> implements ITopologyModifier {
    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingConfigCleanupModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        Map<String, String> lastUserMatches = getLastUserMatches(matchingConfiguration);
        Map<String, List<T>> availableMatches = getAvailableMatches(context);

        // Last user defined matching choices may not be valid anymore so clean up
        // When the user has removed some mapped nodes from the topology the previous substitution configuration still exits.
        Iterator<Entry<String, String>> lastUserSubstitutionsIterator = lastUserMatches.entrySet().iterator();
        while (lastUserSubstitutionsIterator.hasNext()) {
            Map.Entry<String, String> entry = lastUserSubstitutionsIterator.next();
            // The node is still in the topology but we have to check that the existing substitution value is still a valid option.
            List<T> availableSubstitutionsForPolicy = availableMatches.get(entry.getKey());
            if (availableSubstitutionsForPolicy == null) {
                // no options => remove existing mapping
                lastUserSubstitutionsIterator.remove();
                // TODO add log
            } else if (!contains(availableSubstitutionsForPolicy, entry.getValue())) {
                // If the selected value is not a valid choice anymore then remove it
                lastUserSubstitutionsIterator.remove();
                // TODO add log
            }
        }
    }

    protected abstract Map<String, String> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration);

    protected abstract Map<String, List<T>> getAvailableMatches(FlowExecutionContext context);

    private boolean contains(List<T> availableSubstitutionsForPolicy, String subtitutionId) {
        for (T availableSubstitutionForNode : availableSubstitutionsForPolicy) {
            if (availableSubstitutionForNode.getId().equals(subtitutionId)) {
                return true;
            }
        }
        return false;
    }
}
