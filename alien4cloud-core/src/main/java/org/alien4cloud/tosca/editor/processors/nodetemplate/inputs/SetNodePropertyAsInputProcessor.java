package org.alien4cloud.tosca.editor.processors.nodetemplate.inputs;

import static alien4cloud.utils.AlienUtils.getOrFail;

import java.util.Arrays;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.context.ToscaContext;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Set the value of a given property to a get_input function to associate the property to the given input.
 */
@Slf4j
@Component
public class SetNodePropertyAsInputProcessor extends AbstractNodeProcessor<SetNodePropertyAsInputOperation> {
    @Override
    protected void processNodeOperation(Csar csar, Topology topology, SetNodePropertyAsInputOperation operation, NodeTemplate nodeTemplate) {
        PropertyDefinition inputPropertyDefinition = getOrFail(topology.getInputs(), operation.getInputName(), "Input {} not found in topology",
                operation.getInputName());
        NodeType indexedNodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
        PropertyDefinition nodePropertyDefinition = getOrFail(indexedNodeType.getProperties(), operation.getPropertyName(),
                "Property {} do not exist for node {}", operation.getPropertyName(), operation.getNodeName());

        // Check that the property definition of the input is indeed compatible with the property definition of the capability.
        inputPropertyDefinition.checkIfCompatibleOrFail(nodePropertyDefinition);

        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(operation.getInputName()));
        nodeTemplate.getProperties().put(operation.getPropertyName(), getInput);

        log.debug("Associate the property [ {} ] of the node template [ {} ] to input [ {} ] of the topology [ {} ].", operation.getPropertyName(),
                operation.getNodeName(), operation.getInputName(), topology.getId());
    }
}