package org.alien4cloud.tosca.editor.processors.secrets;

import static alien4cloud.utils.AlienUtils.getOrFail;

import java.util.Arrays;

import alien4cloud.utils.services.PropertyService;
import org.alien4cloud.tosca.editor.exception.InvalidSecretPathException;
import org.alien4cloud.tosca.editor.exception.UnsupportedSecretException;
import org.alien4cloud.tosca.editor.operations.secrets.SetNodePropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.context.ToscaContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Set the value of a given property to a get_secret function.
 */
@Slf4j
@Component
public class SetNodePropertyAsSecretProcessor extends AbstractNodeProcessor<SetNodePropertyAsSecretOperation> {

    private final String FORBIDDEN_PROPERTY = "component_version";

    @Override
    protected void processNodeOperation(Csar csar, Topology topology, SetNodePropertyAsSecretOperation operation, NodeTemplate nodeTemplate) {
        NodeType indexedNodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
        getOrFail(indexedNodeType.getProperties(), operation.getPropertyName(), "Property {} do not exist for node {}", operation.getPropertyName(),
                operation.getNodeName());

        if (operation.getPropertyName().equals(FORBIDDEN_PROPERTY)) {
            throw new UnsupportedSecretException("We cannot set a secret on the property " + operation.getPropertyName());
        }

        if ("".equals(operation.getSecretPath())) {
            throw new InvalidSecretPathException("The secret path to the property " + operation.getPropertyName() + " is null.");
        }

        FunctionPropertyValue getSecret = new FunctionPropertyValue();
        getSecret.setFunction(ToscaFunctionConstants.GET_SECRET);
        getSecret.setParameters(Arrays.asList(operation.getSecretPath()));
        nodeTemplate.getProperties().put(operation.getPropertyName(), getSecret);

        log.debug("Associate the property [ {} ] of the node template [ {} ] as secret [ {} ] of the topology [ {} ].", operation.getPropertyName(),
                operation.getNodeName(), operation.getSecretPath(), topology.getId());
    }
}