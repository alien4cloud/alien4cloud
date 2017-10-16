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
 * Modifier to be injected in the flow after the MatchingModifier (policy or node) to select and apply a specific matching for a node.
 */
public abstract class AbstractSetMatchedModifier implements ITopologyModifier {
    /* id of the template (nodetemplate, policyTemplate) */
    private String templateId;
    private String resourceTemplateId;
    // Flag to know if the flow has reach the execution of the set matched node modifier.
    @Getter
    private boolean executed = false;

    public AbstractSetMatchedModifier(String templateId, String resourceTemplateId) {
        this.templateId = templateId;
        this.resourceTemplateId = resourceTemplateId;
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
        Map<String, String> lastUserSubstitutions = getLastUserMatches(matchingConfiguration);
        Map<String, Set<String>> templateIdToAvailableSubstitutions = getAvailableSubstitutions(context);

        // Check the provided resourceTemplate is a valid substitution match and update matching configuration
        Set<String> availableSubstitutions = templateIdToAvailableSubstitutions.get(templateId);
        if (safe(availableSubstitutions).contains(resourceTemplateId)) {
            lastUserSubstitutions.put(templateId, resourceTemplateId);
            context.saveConfiguration(matchingConfiguration);
            return;
        }

        throw new NotFoundException(
                "Requested substitution <" + resourceTemplateId + "> for " + getSubject() + " <" + templateId + "> is not available as a match.");
    }

    abstract Map<String, String> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration);

    abstract Map<String, Set<String>> getAvailableSubstitutions(FlowExecutionContext context);

    abstract String getSubject();
}