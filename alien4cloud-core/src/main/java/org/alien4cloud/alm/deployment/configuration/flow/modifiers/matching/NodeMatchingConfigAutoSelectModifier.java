package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.topology.task.NodeMatchingTask;

/**
 * This node matching modifier is responsible for automatic selection of location resources that are not yet selected by the user.
 * 
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
@Component
public class NodeMatchingConfigAutoSelectModifier implements ITopologyModifier {
    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingConfigAutoSelectModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        Map<String, String> lastUserSubstitutions = matchingConfiguration.getMatchedLocationResources();

        Map<String, List<LocationResourceTemplate>> availableSubstitutions = (Map<String, List<LocationResourceTemplate>>) context.getExecutionCache()
                .get(FlowExecutionContext.MATCHING_PER_NODE_LOC_RES_TEMPLATES);

        // Last user substitution may be incomplete or not valid anymore so let's check them and eventually select default values
        Map<String, LocationResourceTemplate> allAvailableResourceTemplate = Maps.newHashMap();
        Map<String, Set<String>> locResTemplateIdsPerNodeIds = Maps.newHashMap(); // map of nodeId -> location resource template ids required to create
        // historical deployment topology dto object
        for (Map.Entry<String, List<LocationResourceTemplate>> entry : availableSubstitutions.entrySet()) {
            // Fill locResTemplateIdsPerNodeIds
            Set<String> lrtIds = Sets.newHashSet();
            locResTemplateIdsPerNodeIds.put(entry.getKey(), lrtIds);
            // We leverage the loop to also create a map of resources by id for later usage.
            for (LocationResourceTemplate lrt : entry.getValue()) {
                allAvailableResourceTemplate.put(lrt.getId(), lrt);
                lrtIds.add(lrt.getId());
            }
            // select default values
            if (!lastUserSubstitutions.containsKey(entry.getKey())) {
                if (entry.getValue().isEmpty()) {
                    // warn that no node has been found on the location with the topology criteria
                    context.log().error(new NodeMatchingTask(entry.getKey()));
                } else {
                    // Only take the first element as selected if no configuration has been set before
                    // let an info so the user know that we made a default selection for him
                    context.log().info("Automatic matching for node <" + entry.getKey() + ">");
                    lastUserSubstitutions.put(entry.getKey(), entry.getValue().iterator().next().getId());
                }
            }
        }

        // This is required for next modifier so we cache them here for performance reasons.
        context.getExecutionCache().put(FlowExecutionContext.MATCHED_LOCATION_RESOURCE_TEMPLATES, allAvailableResourceTemplate);
        context.getExecutionCache().put(FlowExecutionContext.MATCHED_LOCATION_RESOURCE_TEMPLATE_IDS_PER_NODE, locResTemplateIdsPerNodeIds);

        matchingConfiguration.setMatchedLocationResources(lastUserSubstitutions);
        // TODO Do that only if updated...
        context.saveConfiguration(matchingConfiguration);
    }
}
