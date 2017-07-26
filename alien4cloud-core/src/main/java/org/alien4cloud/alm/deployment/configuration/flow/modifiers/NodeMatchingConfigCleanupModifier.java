package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;
import org.springframework.stereotype.Component;

/**
 * This modifier cleanup the user matching configuration in case it is not valid anymore based on the choices available (that must be fetched from prior
 * NodeMatchingCandidateModifier execution).
 * 
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
@Component
public class NodeMatchingConfigCleanupModifier implements ITopologyModifier {

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingConfigCleanupModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        Map<String, String> lastUserSubstitutions = matchingConfiguration.getMatchedLocationResources();
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = (Map<String, List<LocationResourceTemplate>>) context.getExecutionCache()
                .get(FlowExecutionContext.MATCHING_PER_NODE_LOC_RES_TEMPLATES);

        // Last user defined matching choices may not be valid anymore so clean up
        // When the user has removed some mapped nodes from the topology the previous substitution configuration still exits.
        Iterator<Entry<String, String>> mappingEntryIterator = lastUserSubstitutions.entrySet().iterator();
        while (mappingEntryIterator.hasNext()) {
            Map.Entry<String, String> entry = mappingEntryIterator.next();
            // The node is still in the topology but we have to check that the existing substitution value is still a valid option.
            List<LocationResourceTemplate> availableSubstitutionsForNode = availableSubstitutions.get(entry.getKey());
            if (availableSubstitutionsForNode == null) {
                // no options => remove existing mapping
                mappingEntryIterator.remove();
                // TODO add log
            } else if (!contains(availableSubstitutionsForNode, entry.getValue())) {
                // If the selected value is not a valid choice anymore then remove it
                mappingEntryIterator.remove();
                // TODO add log
            }
        }
    }

    private boolean contains(List<LocationResourceTemplate> availableSubstitutionsForNode, String subtitutionId) {
        for (LocationResourceTemplate availableSubstitutionForNode : availableSubstitutionsForNode) {
            if (availableSubstitutionForNode.getId().equals(subtitutionId)) {
                return true;
            }
        }
        return false;
    }
}