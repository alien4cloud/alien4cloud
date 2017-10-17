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
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

import com.google.common.collect.Sets;

import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * This modifier applies user defined properties on substituted resources after matching.
 */
@Slf4j
public abstract class AbstractPostMatchingSetupModifier<T extends AbstractTemplate> implements ITopologyModifier {

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                AbstractPostMatchingSetupModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration deploymentMatchingConfiguration = configurationOptional.get();
        Map<String, String> lastUserSubstitutions = getUserMatches(deploymentMatchingConfiguration);
        Map<String, NodePropsOverride> propertiesOverrides = getPropertiesOverrides(deploymentMatchingConfiguration);

        boolean configChanged = false;

        Iterator<Entry<String, NodePropsOverride>> nodePropsOverrideIter = safe(propertiesOverrides).entrySet().iterator();
        while (nodePropsOverrideIter.hasNext()) {
            Entry<String, NodePropsOverride> nodePropsOverrideEntry = nodePropsOverrideIter.next();
            if (lastUserSubstitutions.containsKey(nodePropsOverrideEntry.getKey())) {
                // Merge the user overrides into the node.
                configChanged = mergeNode(topology, context, nodePropsOverrideEntry.getKey(), nodePropsOverrideEntry.getValue());
            } else {
                // This node is no more a matched node, remove from configuration
                configChanged = true;
                context.getLog().info("Definition of user properties for " + getSubject() + " [" + nodePropsOverrideEntry.getKey()
                        + "] have been removed as the " + getSubject() + " is not available for matching anymore.");
                nodePropsOverrideIter.remove();
            }
        }
        // If the configuration has changed then update it.
        if (configChanged) {
            context.saveConfiguration(deploymentMatchingConfiguration);
        }
    }

    private boolean mergeNode(Topology topology, FlowExecutionContext context, String templateId, NodePropsOverride nodePropsOverride) {
        if (nodePropsOverride == null) {
            return false;
        }
        return doMergeNode(topology, context, templateId, nodePropsOverride);
    }

    protected boolean doMergeNode(Topology topology, FlowExecutionContext context, String templateId, NodePropsOverride nodePropsOverride) {
        final ConfigChanged configChanged = new ConfigChanged();
        // This node is still a matched node merge properties
        T template = getTemplates(topology).get(templateId);
        template.setProperties(mergeProperties(nodePropsOverride.getProperties(), template.getProperties(), propertyName -> {
            configChanged.changed = true;
            context.getLog().info("The property [" + propertyName + "] previously specified to configure " + getSubject() + " [" + templateId
                    + "] cannot be set anymore as it is already specified by the matched location resource or in the topology.");
        }));

        return configChanged.changed;
    }

    protected Map<String, AbstractPropertyValue> mergeProperties(Map<String, AbstractPropertyValue> source, Map<String, AbstractPropertyValue> target,
            Consumer<String> messageConsumer) {
        Set<String> untouchedProperties = Sets.newHashSet();
        Map<String, AbstractPropertyValue> merged = CollectionUtils.merge(source, target, true, untouchedProperties);
        for (String untouchedProp : untouchedProperties) {
            messageConsumer.accept(untouchedProp);
            source.remove(untouchedProp);
        }
        return merged;
    }

    protected static class ConfigChanged {
        protected boolean changed = false;
    }

    abstract Map<String, String> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration);

    abstract Map<String, NodePropsOverride> getPropertiesOverrides(DeploymentMatchingConfiguration matchingConfiguration);

    abstract String getSubject();

    abstract Map<String, T> getTemplates(Topology topology);
}