package org.alien4cloud.alm.deployment.configuration.flow.modifiers.action;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.NodeMatchingConfigAutoSelectModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.task.LocationPolicyTask;
import lombok.Getter;

/**
 * Modifier to be injected in the flow after the NodeMatchingModifier to select and apply a specific matching for a node.
 */
public class SetMatchedNodeModifier implements ITopologyModifier {
    private String nodeId;
    private String locationResourceTemplateId;
    // Flag to know if the flow has reach the execution of the set matched node modifier.
    @Getter
    private boolean executed = false;

    public SetMatchedNodeModifier(String nodeId, String locationResourceTemplateId) {
        this.nodeId = nodeId;
        this.locationResourceTemplateId = locationResourceTemplateId;
    }

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        executed = true;
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingConfigAutoSelectModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        Map<String, String> lastUserSubstitutions = matchingConfiguration.getMatchedLocationResources();

        Map<String, Set<String>> locResTemplateIdsPerNodeIds = (Map<String, Set<String>>) context.getExecutionCache()
                .get(FlowExecutionContext.SELECTED_MATCH_NODE_LOCATION_TEMPLATE_BY_NODE_ID_MAP);

        // Update matching configuration
        Set<String> nodeAvailableSubstitutions = locResTemplateIdsPerNodeIds.get(nodeId);
        for (String matchedLRTId : safe(nodeAvailableSubstitutions)) {
            if (matchedLRTId.equals(locationResourceTemplateId)) {
                lastUserSubstitutions.put(nodeId, locationResourceTemplateId);
                context.saveConfiguration(matchingConfiguration);
                return;
            }
        }

        throw new NotFoundException("Requested substitution <" + locationResourceTemplateId + "> for node <" + nodeId + "> is not available as a match.");
    }
}