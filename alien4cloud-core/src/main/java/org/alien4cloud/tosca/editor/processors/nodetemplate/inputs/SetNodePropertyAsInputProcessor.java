package org.alien4cloud.tosca.editor.processors.nodetemplate.inputs;

import static alien4cloud.utils.AlienUtils.getOrFail;

import java.util.Arrays;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Set the value of a given property to a get_input function to associate the property to the given input.
 */
@Slf4j
@Component
public class SetNodePropertyAsInputProcessor extends AbstractNodeProcessor<SetNodePropertyAsInputOperation> {
    @Override
    protected void processNodeOperation(SetNodePropertyAsInputOperation operation, NodeTemplate nodeTemplate) {
        Topology topology = EditionContextManager.getTopology();

        PropertyDefinition inputPropertyDefinition = getOrFail(topology.getInputs(), operation.getInputName(), "Input {} not found in topology",
                operation.getInputName());
        IndexedNodeType indexedNodeType = ToscaContext.get(IndexedNodeType.class, nodeTemplate.getType());
        PropertyDefinition nodePropertyDefinition = getOrFail(indexedNodeType.getProperties(), operation.getPropertyName(),
                "Property {} do not exist for node {}", operation.getPropertyName(), operation.getNodeName());

        // Check that the property definition of the input is indeed compatible with the property definition of the capability.
        inputPropertyDefinition.checkIfCompatibleOrFail(nodePropertyDefinition);

        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(operation.getInputName()));
        nodeTemplate.getProperties().put(operation.getPropertyName(), getInput);

        log.debug("Associate the property <{}> of the node template <{}> to input <{}> of the topology <{}>.", operation.getPropertyName(),
                operation.getNodeName(), operation.getInputName(), topology.getId());
    }
}