package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import alien4cloud.topology.task.TaskCode;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

import com.google.common.collect.Sets;

import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.topology.task.LocationPolicyTask;
import org.springframework.stereotype.Component;

/**
 * Specific modifier used as a workaround for cloudify plugin as cloudify does not support multiple relationships between 2 nodes.
 *
 * FIXME remove from base code when modifiers can be injected based on the selected location.
 */
@Component
public class CfyMultirelationshipErrorModifier implements ITopologyModifier {
    @Inject
    private OrchestratorService orchestratorService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        // Check if orchestrator is cloudify
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                LocationMatchingModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) {
            context.log().error(new LocationPolicyTask());
            return;
        }
        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        Orchestrator orchestrator = orchestratorService.getOrFail(matchingConfiguration.getOrchestratorId());
        if (!orchestrator.getPluginId().contains("cloudify")) {
            return;
        }

        // For every node check that there is not two relationships defined
        for (Entry<String, NodeTemplate> nodeTemplateEntry : safe(topology.getNodeTemplates()).entrySet()) {
            // Keep track of the relationship id
            Set<String> relationshipTargets = Sets.newHashSet();
            for (Entry<String, RelationshipTemplate> relationshipTemplateEntry : safe(nodeTemplateEntry.getValue().getRelationships()).entrySet()) {
                if (relationshipTargets.contains(relationshipTemplateEntry.getValue().getTarget())) {
                    context.log().error(TaskCode.CFY_MULTI_RELATIONS,
                            "Cloudify orchestrator does not support multiple relationships between the same source and target nodes. Topology defines more than one between "
                                    + nodeTemplateEntry.getKey() + " and " + relationshipTemplateEntry.getValue().getTarget() + ".");
                    return;
                }
                relationshipTargets.add(relationshipTemplateEntry.getValue().getTarget());
            }
        }
    }
}
