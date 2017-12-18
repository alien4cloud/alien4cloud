package org.alien4cloud.tosca.editor.processors.secrets;

import static alien4cloud.utils.AlienUtils.getOrFail;

import java.util.Arrays;

import org.alien4cloud.tosca.editor.operations.secrets.SetRelationshipPropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.processors.relationshiptemplate.AbstractRelationshipProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.context.ToscaContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Set the value of a given relationship to a get_secret function.
 */
@Slf4j
@Component
public class SetRelationshipPropertyAsSecretProcessor extends AbstractRelationshipProcessor<SetRelationshipPropertyAsSecretOperation> {

    @Override
    protected void processRelationshipOperation(Csar csar, Topology topology, SetRelationshipPropertyAsSecretOperation operation, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate) {

        RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
        PropertyDefinition relationshipPropertyDefinition = getOrFail(relationshipType.getProperties(), operation.getPropertyName(),
                "Property {} do not exist for relationship {} of node {}", operation.getPropertyName(), operation.getRelationshipName(),
                operation.getNodeName());

        FunctionPropertyValue secretFunction = new FunctionPropertyValue();
        secretFunction.setFunction(ToscaFunctionConstants.GET_SECRET);
        secretFunction.setParameters(Arrays.asList(operation.getSecretPath()));
        relationshipTemplate.getProperties().put(operation.getPropertyName(), secretFunction);

        log.debug("Associate the property [ {} ] of relationship template [ {} ] of node [ {} ] to secret <path: {} > of the topology [ {} ].", operation.getPropertyName(),
                operation.getRelationshipName(), operation.getNodeName(), operation.getSecretPath(), topology.getId());
    }
}
