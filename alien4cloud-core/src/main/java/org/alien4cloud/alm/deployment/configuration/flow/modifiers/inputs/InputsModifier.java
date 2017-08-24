package org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.alm.deployment.configuration.services.InputService;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Requirement;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.utils.FunctionEvaluator;
import org.alien4cloud.tosca.utils.FunctionEvaluatorContext;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.DeploymentInputService;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;

/**
 * Inputs processor modifier performs update of the topology based on inputs provided by the deployer user or from application meta properties and tags values.
 *
 * Note that location meta-properties related inputs are filled by another modifier (LocationInputsProcessorModifier) as they are not filled-in in the same step
 * in the flow.
 */
@Component
public class InputsModifier implements ITopologyModifier {
    @Inject
    private InputService inputService;
    @Inject
    private DeploymentInputService deploymentInputService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        ApplicationEnvironment environment = context.getEnvironmentContext()
                .orElseThrow(() -> new IllegalArgumentException("Input modifier requires an environment context.")).getEnvironment();
        DeploymentInputs deploymentInputs = context.getConfiguration(DeploymentInputs.class, InputsModifier.class.getSimpleName())
                .orElse(new DeploymentInputs(environment.getTopologyVersion(), environment.getId()));
        if(deploymentInputs.getInputs() == null) {
            deploymentInputs.setInputs(Maps.newHashMap());
        }

        Map<String, Location> locations = (Map<String, Location>) context.getExecutionCache().get(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY);
        Map<String, PropertyValue> applicationInputs = inputService.getAppContextualInputs(context.getEnvironmentContext().get().getApplication(),
                topology.getInputs());
        Map<String, PropertyValue> locationInputs = inputService.getLocationContextualInputs(locations, topology.getInputs());

        // If the initial topology or any modifier configuration has changed since last inputs update then we refresh inputs.
        if (deploymentInputs.getLastUpdateDate() == null || deploymentInputs.getLastUpdateDate().before(context.getLastFlowParamUpdate())) {
            // FIXME exclude the application and location provided inputs from this method as it process them...
            boolean updated = deploymentInputService.synchronizeInputs(topology.getInputs(), deploymentInputs.getInputs());
            if (updated) {
                // save the config if changed. This is for ex, if an input has been deleted from the topology
                context.saveConfiguration(deploymentInputs);
            }
        }

        Map<String, PropertyValue> inputValues = deploymentInputs.getInputs();
        inputValues.putAll(applicationInputs);
        inputValues.putAll(locationInputs);

        // Now that we have inputs ready let's process get_input functions in the topology to actually replace values.
        if (topology.getNodeTemplates() != null) {
            FunctionEvaluatorContext evaluatorContext = new FunctionEvaluatorContext(topology, inputValues);

            for (Entry<String, NodeTemplate> entry : topology.getNodeTemplates().entrySet()) {
                NodeTemplate nodeTemplate = entry.getValue();
                processGetInput(evaluatorContext, nodeTemplate, nodeTemplate.getProperties());

                if (nodeTemplate.getRelationships() != null) {
                    for (Entry<String, RelationshipTemplate> relEntry : nodeTemplate.getRelationships().entrySet()) {
                        RelationshipTemplate relationshipTemplate = relEntry.getValue();
                        processGetInput(evaluatorContext, relationshipTemplate, relationshipTemplate.getProperties());
                    }
                }
                if (nodeTemplate.getCapabilities() != null) {
                    for (Entry<String, Capability> capaEntry : nodeTemplate.getCapabilities().entrySet()) {
                        Capability capability = capaEntry.getValue();
                        processGetInput(evaluatorContext, nodeTemplate, capability.getProperties());
                    }
                }
                if (nodeTemplate.getRequirements() != null) {
                    for (Entry<String, Requirement> requirementEntry : nodeTemplate.getRequirements().entrySet()) {
                        Requirement requirement = requirementEntry.getValue();
                        processGetInput(evaluatorContext, nodeTemplate, requirement.getProperties());
                    }
                }
            }
        }
    }

    private void processGetInput(FunctionEvaluatorContext evaluatorContext, AbstractTemplate template, Map<String, AbstractPropertyValue> properties) {
        for (Map.Entry<String, AbstractPropertyValue> propEntry : safe(properties).entrySet()) {
            try {
                PropertyValue value = FunctionEvaluator.resolveValue(evaluatorContext, template, properties, propEntry.getValue());
                propEntry.setValue(value);
            } catch (IllegalArgumentException e) {
                // FIXME we should add an error log rather than throwing the exception here. See internal of FunctionEvaluator.resolveValue and especially
                // concat processing.
                throw e;
            }
        }
    }
}