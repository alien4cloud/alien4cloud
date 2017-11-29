package org.alien4cloud.tosca.editor.processors.secrets;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.UnsetNodePropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import static alien4cloud.paas.function.FunctionEvaluator.isGetSecret;
import static alien4cloud.utils.AlienUtils.getOrFail;

/**
 * Remove secret in a node template's property.
 */
@Slf4j
@Component
public class UnsetNodePropertyAsSecretProcessor extends AbstractNodeProcessor<UnsetNodePropertyAsSecretOperation> {
    @Override
    protected void processNodeOperation(Csar csar, Topology topology, UnsetNodePropertyAsSecretOperation operation, NodeTemplate nodeTemplate) {
        // check if the node property value is a get_secret
        AbstractPropertyValue currentValue = nodeTemplate.getProperties().get(operation.getPropertyName());
        if (currentValue != null && !isGetSecret(currentValue)) {
            throw new NotFoundException("Property {} of node {} is not associated to an secret.", operation.getPropertyName(), operation.getNodeName());
        }

        NodeType nodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
        PropertyDefinition nodePropertyDefinition = getOrFail(nodeType.getProperties(), operation.getPropertyName(), "Property {} do not exist for node {}",
                operation.getPropertyName(), operation.getNodeName());

        AbstractPropertyValue defaultPropertyValue = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(nodePropertyDefinition);
        nodeTemplate.getProperties().put(operation.getPropertyName(), defaultPropertyValue);

        log.debug("Remove secret property [ {} ] of the node template [ {} ] of the topology [ {} ].", operation.getPropertyName(),
                operation.getNodeName(), topology.getId());
    }
}
