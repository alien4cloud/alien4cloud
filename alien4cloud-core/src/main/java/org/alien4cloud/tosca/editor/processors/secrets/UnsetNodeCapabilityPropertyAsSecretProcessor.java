package org.alien4cloud.tosca.editor.processors.secrets;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.UnsetNodeCapabilityPropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.springframework.stereotype.Component;

import static alien4cloud.paas.function.FunctionEvaluator.isGetSecret;
import static alien4cloud.utils.AlienUtils.getOrFail;

/**
 * Remove secret property of a node template's capability.
 */
@Slf4j
@Component
public class UnsetNodeCapabilityPropertyAsSecretProcessor extends AbstractNodeProcessor<UnsetNodeCapabilityPropertyAsSecretOperation> {
    @Override
    protected void processNodeOperation(Csar csar, Topology topology, UnsetNodeCapabilityPropertyAsSecretOperation operation, NodeTemplate nodeTemplate) {
        Capability capabilityTemplate = getOrFail(nodeTemplate.getCapabilities(), operation.getCapabilityName(), "Capability {} do not exist for node {}",
                operation.getCapabilityName(), operation.getNodeName());

        // check if the node property value is a get_secret
        AbstractPropertyValue currentValue = capabilityTemplate.getProperties().get(operation.getPropertyName());
        if (!isGetSecret(currentValue)) {
            throw new NotFoundException("Property {} of node {} is not an secret.", operation.getPropertyName(), operation.getNodeName());
        }

        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capabilityTemplate.getType());
        PropertyDefinition capabilityPropertyDefinition = getOrFail(capabilityType.getProperties(), operation.getPropertyName(),
                "Property {} do not exist for capability {} of node {}", operation.getPropertyName(), operation.getCapabilityName(), operation.getNodeName());

        AbstractPropertyValue defaultPropertyValue = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(capabilityPropertyDefinition);
        capabilityTemplate.getProperties().put(operation.getPropertyName(), defaultPropertyValue);

        log.debug("Remove secret property [ {} ] of capability template [ {} ] of node [ {} ] of the topology [ {} ].",
                operation.getPropertyName(), operation.getCapabilityName(), operation.getNodeName(), topology.getId());
    }
}
