package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.SetNodePropertyAsInputOperation;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Set the value of a given property to a get_input function to associate the property to the given input.
 */
@Slf4j
@Component
public class SetNodePropertyAsInputProcessor extends AbstractNodeProcessor<SetNodePropertyAsInputOperation> {
    @Inject
    private TopologyServiceCore topologyServiceCore;

    @Override
    protected void processNodeOperation(SetNodePropertyAsInputOperation operation, NodeTemplate nodeTemplate) {
        Topology topology = EditionContextManager.getTopology();
        Map<String, PropertyDefinition> inputs = topology.getInputs();
        if (inputs == null || !inputs.containsKey(operation.getInputName())) {
            throw new NotFoundException("Input " + operation.getInputName() + "not found in topology");
        }

        IndexedNodeType indexedNodeType = ToscaContext.get(IndexedNodeType.class, nodeTemplate.getType());
        PropertyDefinition propertyDefinition = inputs.get(operation.getInputName());
        propertyDefinition.checkIfCompatibleOrFail(indexedNodeType.getProperties().get(operation.getPropertyName()));

        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(operation.getInputName()));
        nodeTemplate.getProperties().put(operation.getPropertyName(), getInput);
        topology.setInputs(inputs);

        log.debug("Associate the property <{}> of the node template <{}> to input <{}> of the topology <{}>.", operation.getPropertyName(),
                operation.getNodeName(), operation.getInputName(), topology.getId());
        topologyServiceCore.save(topology);
    }
}