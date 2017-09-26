package org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs;

import alien4cloud.deployment.DeploymentInputArtifactValidationService;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.alm.deployment.configuration.model.PreconfiguredInputsConfiguration;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Topology modifier that performs input validation
 */
@Component
public class InputValidationModifier implements ITopologyModifier {
    @Inject
    private DeploymentInputArtifactValidationService deploymentInputArtifactValidationService;

    /**
     * Validate all required input is provided with a non null value.
     * 
     * @param topology The topology to process.
     * @param context The object that stores warnings and errors (tasks) associated with the execution flow. Note that the flow will end-up if an error
     */
    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentInputs> inputsOptional = context.getConfiguration(DeploymentInputs.class, InputValidationModifier.class.getSimpleName());
        PreconfiguredInputsConfiguration preconfiguredInputsConfiguration = context.getConfiguration(PreconfiguredInputsConfiguration.class, InputValidationModifier.class.getSimpleName())
                .orElseThrow(() -> new IllegalStateException("PreconfiguredInputsConfiguration must be in the context"));

        // Define a task regarding properties
        PropertiesTask task = new PropertiesTask();
        task.setCode(TaskCode.INPUT_PROPERTY);
        task.setProperties(Maps.newHashMap());
        task.getProperties().put(TaskLevel.REQUIRED, Lists.newArrayList());
        Map<String, PropertyValue> inputValues = safe(inputsOptional.orElse(new DeploymentInputs()).getInputs());
        Map<String, PropertyValue> predefinedInputValues = safe(preconfiguredInputsConfiguration.getInputs());

        // override deployer inputValues with predefinedInputValues
        inputValues = Maps.newHashMap(inputValues);
        inputValues.putAll(predefinedInputValues);

        for (Entry<String, PropertyDefinition> propDef : safe(topology.getInputs()).entrySet()) {
            if (propDef.getValue().isRequired() && inputValues.get(propDef.getKey()) == null) {
                task.getProperties().get(TaskLevel.REQUIRED).add(propDef.getKey());
            }
        }

        if (CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.REQUIRED))) {
            context.log().error(task);
        }

        // Check input artifacts
        deploymentInputArtifactValidationService.validate(topology, inputsOptional.orElse(new DeploymentInputs()))
                .forEach(inputArtifactTask -> context.log().error(inputArtifactTask));
    }
}
