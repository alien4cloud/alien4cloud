package org.alien4cloud.tosca.editor.processors.nodetemplate.inputs;

import static alien4cloud.utils.AlienUtils.getOrFail;

import java.util.Arrays;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.context.ToscaContext;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Associate the property of a node's capability to an input.
 */
@Slf4j
@Component
public class SetNodeCapabilityPropertyAsInputProcessor extends AbstractNodeProcessor<SetNodeCapabilityPropertyAsInputOperation> {

    @Override
    protected void processNodeOperation(Csar csar, Topology topology, SetNodeCapabilityPropertyAsInputOperation operation, NodeTemplate nodeTemplate) {
        Capability capabilityTemplate = getOrFail(nodeTemplate.getCapabilities(), operation.getCapabilityName(), "Capability {} does not exist for node {}",
                operation.getCapabilityName(), operation.getNodeName());
        PropertyDefinition inputPropertyDefinition = getOrFail(topology.getInputs(), operation.getInputName(), "Input {} not found in topology",
                operation.getInputName());

        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capabilityTemplate.getType());
        PropertyDefinition capabilityPropertyDefinition = getOrFail(capabilityType.getProperties(), operation.getPropertyName(),
                "Property {} do not exist for capability {} of node {}", operation.getPropertyName(), operation.getCapabilityName(), operation.getNodeName());

        // Check that the property definition of the input is indeed compatible with the property definition of the capability.
        inputPropertyDefinition.checkIfCompatibleOrFail(capabilityPropertyDefinition);

        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(operation.getInputName()));
        capabilityTemplate.getProperties().put(operation.getPropertyName(), getInput);

        log.debug("Associate the property [ {} ] of capability template [ {} ] of node [ {} ] to an input of the topology [ {} ].", operation.getPropertyName(),
                operation.getCapabilityName(), operation.getNodeName(), topology.getId());
    }
}
