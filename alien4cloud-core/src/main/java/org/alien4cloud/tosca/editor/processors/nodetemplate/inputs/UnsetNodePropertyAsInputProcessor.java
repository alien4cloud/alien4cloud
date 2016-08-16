package org.alien4cloud.tosca.editor.processors.nodetemplate.inputs;

import static alien4cloud.paas.function.FunctionEvaluator.isGetInput;
import static alien4cloud.utils.AlienUtils.getOrFail;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodePropertyAsInputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Remove association to an input in a node template's property.
 */
@Slf4j
@Component
public class UnsetNodePropertyAsInputProcessor extends AbstractNodeProcessor<UnsetNodePropertyAsInputOperation> {
    @Override
    protected void processNodeOperation(UnsetNodePropertyAsInputOperation operation, NodeTemplate nodeTemplate) {
        // check if the node property value is a get_input
        AbstractPropertyValue currentValue = nodeTemplate.getProperties().get(operation.getPropertyName());
        if (!isGetInput(currentValue)) {
            throw new NotFoundException("Property {} of node {} is not associated to an input.", operation.getPropertyName(), operation.getNodeName());
        }

        IndexedNodeType nodeType = ToscaContext.get(IndexedNodeType.class, nodeTemplate.getType());
        PropertyDefinition nodePropertyDefinition = getOrFail(nodeType.getProperties(), operation.getPropertyName(), "Property {} do not exist for node {}",
                operation.getPropertyName(), operation.getNodeName());

        AbstractPropertyValue defaultPropertyValue = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(nodePropertyDefinition);
        nodeTemplate.getProperties().put(operation.getPropertyName(), defaultPropertyValue);

        log.debug("Remove association from property <{}> of the node template <{}> to an input of the topology <{}>.", operation.getPropertyName(),
                operation.getNodeName(), EditionContextManager.getTopology().getId());
    }
}
