package org.alien4cloud.tosca.editor.processors.secrets;

import alien4cloud.tosca.context.ToscaContext;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.exception.UnsupportedSecretException;
import org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.SetNodeCapabilityPropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static alien4cloud.utils.AlienUtils.getOrFail;

/**
 * Associate the property of a node's capability to an secret.
 */
@Slf4j
@Component
public class SetNodeCapabilityPropertyAsSecretProcessor extends AbstractNodeProcessor<SetNodeCapabilityPropertyAsSecretOperation> {

    private final String forbiddenCapability = "component_version";

    @Override
    protected void processNodeOperation(Csar csar, Topology topology, SetNodeCapabilityPropertyAsSecretOperation operation, NodeTemplate nodeTemplate) {
        Capability capabilityTemplate = getOrFail(nodeTemplate.getCapabilities(), operation.getCapabilityName(), "Capability {} does not exist for node {}",
                operation.getCapabilityName(), operation.getNodeName());
        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capabilityTemplate.getType());
        getOrFail(capabilityType.getProperties(), operation.getPropertyName(),
                "Property {} do not exist for capability {} of node {}", operation.getPropertyName(), operation.getCapabilityName(), operation.getNodeName());

        if (operation.getCapabilityName().equals(forbiddenCapability)) {
            throw new UnsupportedSecretException("We cannot set a secret on the capability " + operation.getCapabilityName());
        }

        FunctionPropertyValue getSecret = new FunctionPropertyValue();
        getSecret.setFunction(ToscaFunctionConstants.GET_SECRET);
        getSecret.setParameters(Arrays.asList(operation.getSecretPath()));
        nodeTemplate.getProperties().put(operation.getPropertyName(), getSecret);

        log.debug("Set the property [ {} ] of capability template [ {} ] of node [ {} ] to the secret path [ {} ].", operation.getPropertyName(),
                operation.getCapabilityName(), operation.getNodeName(), topology.getId());
    }
}
