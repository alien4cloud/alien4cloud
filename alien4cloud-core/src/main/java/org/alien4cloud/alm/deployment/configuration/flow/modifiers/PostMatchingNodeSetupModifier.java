package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodeCapabilitiesPropsOverride;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.ResourceMatching;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.context.ToscaContext;
import lombok.extern.slf4j.Slf4j;

/**
 * This modifier applies user defined properties on substituted node after matching.
 */
@Slf4j
@Component
public class PostMatchingNodeSetupModifier extends AbstractPostMatchingSetupModifier<NodeType, NodeTemplate> {

    @Override
    protected boolean doMergeNode(Topology topology, FlowExecutionContext context, String nodeTemplateId, NodePropsOverride nodePropsOverride) {

        final ConfigChanged configChanged = new ConfigChanged();

        // play the super method first. This will process nodetemplate properties
        configChanged.changed = super.doMergeNode(topology, context, nodeTemplateId, nodePropsOverride);

        // Then process capabilities
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateId);
        Iterator<Entry<String, NodeCapabilitiesPropsOverride>> capabilitiesOverrideIter = safe(nodePropsOverride.getCapabilities()).entrySet().iterator();
        while (capabilitiesOverrideIter.hasNext()) {
            Entry<String, NodeCapabilitiesPropsOverride> overrideCapabilityProperties = capabilitiesOverrideIter.next();
            Capability capability = safe(nodeTemplate.getCapabilities()).get(overrideCapabilityProperties.getKey());
            if (capability == null) { // Manage clean logic
                configChanged.changed = true;
                capabilitiesOverrideIter.remove();
            } else { // Merge logic
                // When a selected node has changed we may need to cleanup properties that where defined but are not anymore on the capability
                CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capability.getType());
                capability.setProperties(mergeProperties(overrideCapabilityProperties.getValue().getProperties(), capability.getProperties(),
                        capabilityType.getProperties(), s -> {
                            configChanged.changed = true;
                            context.log()
                                    .info("The property [" + s + "] previously specified to configure capability [" + overrideCapabilityProperties.getKey()
                                            + "] of node [" + nodeTemplateId
                                            + "] cannot be set anymore as it is already specified by the matched location resource or in the topology.");
                        }));
            }
        }
        return configChanged.changed;
    }

    @Override
    Map<String, ResourceMatching> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedLocationResources();
    }

    @Override
    Map<String, NodePropsOverride> getPropertiesOverrides(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedNodesConfiguration();
    }

    @Override
    String getSubject() {
        return "node";
    }

    @Override
    Map<String, NodeTemplate> getTemplates(Topology topology) {
        return topology.getNodeTemplates();
    }

    @Override
    Class<NodeType> getToscaTypeClass() {
        return NodeType.class;
    }
}