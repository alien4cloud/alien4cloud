package org.alien4cloud.tosca.editor.processors.relationshiptemplate.inputs;

import static alien4cloud.utils.AlienUtils.getOrFail;

import java.util.Arrays;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.SetRelationshipPropertyAsInputOperation;
import org.alien4cloud.tosca.editor.processors.relationshiptemplate.AbstractRelationshipProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
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

        IndexedRelationshipType relationshipType = ToscaContext.get(IndexedRelationshipType.class, relationshipTemplate.getType());
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