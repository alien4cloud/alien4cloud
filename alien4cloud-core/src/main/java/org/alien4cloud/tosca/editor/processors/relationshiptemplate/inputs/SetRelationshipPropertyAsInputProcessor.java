package org.alien4cloud.tosca.editor.processors.relationshiptemplate.inputs;

import static alien4cloud.utils.AlienUtils.getOrFail;

import java.util.Arrays;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.SetRelationshipPropertyAsInputOperation;
import org.alien4cloud.tosca.editor.processors.relationshiptemplate.AbstractRelationshipProcessor;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Associate the property of a node's relationship to an input.
 */
@Slf4j
@Component
public class SetRelationshipPropertyAsInputProcessor extends AbstractRelationshipProcessor<SetRelationshipPropertyAsInputOperation> {
    @Override
    protected void processRelationshipOperation(SetRelationshipPropertyAsInputOperation operation, NodeTemplate nodeTemplate,
            RelationshipTemplate relationshipTemplate) {
        Topology topology = EditionContextManager.getTopology();
        PropertyDefinition inputPropertyDefinition = getOrFail(topology.getInputs(), operation.getInputName(), "Input {} not found in topology",
                operation.getInputName());

        RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
        PropertyDefinition relationshipPropertyDefinition = getOrFail(relationshipType.getProperties(), operation.getPropertyName(),
                "Property {} do not exist for relationship {} of node {}", operation.getPropertyName(), operation.getRelationshipName(),
                operation.getNodeName());

        // Check that the property definition of the input is indeed compatible with the property definition of the capability.
        inputPropertyDefinition.checkIfCompatibleOrFail(relationshipPropertyDefinition);

        FunctionPropertyValue getInput = new FunctionPropertyValue();
        getInput.setFunction(ToscaFunctionConstants.GET_INPUT);
        getInput.setParameters(Arrays.asList(operation.getInputName()));
        relationshipTemplate.getProperties().put(operation.getPropertyName(), getInput);

        log.debug("Associate the property <{}> of relationship template <{}> of node <{}> to an input of the topology <{}>.", operation.getPropertyName(),
                operation.getRelationshipName(), operation.getNodeName(), EditionContextManager.getTopology().getId());
    }
}