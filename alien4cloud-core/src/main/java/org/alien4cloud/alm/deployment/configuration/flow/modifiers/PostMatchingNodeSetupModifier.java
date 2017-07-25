package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodeCapabilitiesPropsOverride;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * This modifier applies user defined properties on subtituted node after matching.
 */
@Slf4j
@Component
public class PostMatchingNodeSetupModifier implements ITopologyModifier {

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                PostMatchingNodeSetupModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration deploymentMatchingConfiguration = configurationOptional.get();
        Map<String, String> lastUserSubstitutions = deploymentMatchingConfiguration.getMatchedLocationResources();
        Map<String, NodePropsOverride> matchedNodesConfiguration = deploymentMatchingConfiguration.getMatchedNodesConfiguration();

        boolean configChanged = false;

        Iterator<Entry<String, NodePropsOverride>> nodePropsOverrideIter = matchedNodesConfiguration.entrySet().iterator();
        while (nodePropsOverrideIter.hasNext()) {
            Entry<String, NodePropsOverride> nodePropsOverrideEntry = nodePropsOverrideIter.next();
            if (lastUserSubstitutions.containsKey(nodePropsOverrideEntry.getKey())) {
                // Merge the user overrides into the node.
                configChanged = mergeNode(topology, context, nodePropsOverrideEntry.getKey(), nodePropsOverrideEntry.getValue());
            } else {
                // This node is no more a matched node, remove from configuration
                configChanged = true;
                context.getLog().info("Definition of user properties for node <" + nodePropsOverrideEntry.getKey()
                        + "> have been removed as the node is not available for matching anymore.");
                nodePropsOverrideIter.remove();
            }
        }
        // If the configuration has changed then update it.
        if (configChanged) {
            context.saveConfiguration(deploymentMatchingConfiguration);
        }
    }

    private boolean mergeNode(Topology topology, FlowExecutionContext context, String nodeTemplateId, NodePropsOverride nodePropsOverride) {
        if (nodePropsOverride == null) {
            return false;
        }
        final ConfigChanged configChanged = new ConfigChanged();
        // This node is still a matched node merge properties
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateId);
        nodeTemplate.setProperties(mergeProperties(nodePropsOverride.getProperties(), nodeTemplate.getProperties(), s -> {
            configChanged.changed = true;
            context.getLog().info("The property <" + s + "> previously specified to configure node <" + nodeTemplateId
                    + "> cannot be set anymore as it is already specified by the location resource or in the topology.");
        }));

        Iterator<Entry<String, NodeCapabilitiesPropsOverride>> capabilitiesOverrideIter = safe(nodePropsOverride.getCapabilities()).entrySet().iterator();
        while (capabilitiesOverrideIter.hasNext()) {
            Entry<String, NodeCapabilitiesPropsOverride> capabilityProperties = capabilitiesOverrideIter.next();
            Capability capability = safe(nodeTemplate.getCapabilities()).get(capabilityProperties.getKey());
            if (capability == null) { // Manage clean logic
                configChanged.changed = true;
                capabilitiesOverrideIter.remove();
            } else { // Merge logic
                capability.setProperties(mergeProperties(capability.getProperties(), capabilityProperties.getValue().getProperties(), s -> {
                    configChanged.changed = true;
                    context.getLog()
                            .info("The property <" + s + "> previously specified to configure capability <" + capabilityProperties.getKey() + "> of node <"
                                    + nodeTemplateId + "> cannot be set anymore as it is already specified by the location resource or in the topology.");
                }));
            }
        }
        return configChanged.changed;
    }

    private Map<String, AbstractPropertyValue> mergeProperties(Map<String, AbstractPropertyValue> source, Map<String, AbstractPropertyValue> target,
            Consumer<String> messageSupplier) {
        Set<String> untouchedProperties = Sets.newHashSet();
        Map<String, AbstractPropertyValue> merged = CollectionUtils.merge(source, target, true, untouchedProperties);
        for (String untouchedProp : untouchedProperties) {
            messageSupplier.accept(untouchedProp);
            source.remove(untouchedProp);
        }
        return merged;
    }

    private static class ConfigChanged {
        private boolean changed = false;
    }
}