package org.alien4cloud.tosca.editor.processors.relationshiptemplate.inputs;

import static alien4cloud.utils.AlienUtils.getOrFail;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.UnsetRelationshipPropertyAsInputOperation;
import org.alien4cloud.tosca.editor.processors.relationshiptemplate.AbstractRelationshipProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Remove association to an input in the property of a node template's relationship.
 */
@Slf4j
@Component
public class UnsetRelationshipPropertyAsInputProcessor extends AbstractRelationshipProcessor<UnsetRelationshipPropertyAsInputOperation> {
    @Override
    protected void processRelationshipOperation(UnsetRelationshipPropertyAsInputOperation operation, NodeTemplate nodeTemplate,
            RelationshipTemplate relationshipTemplate) {

        IndexedRelationshipType relationshipType = ToscaContext.get(IndexedRelationshipType.class, relationshipTemplate.getType());
        PropertyDefinition relationshipPropertyDefinition = getOrFail(relationshipType.getProperties(), operation.getPropertyName(),
                "Property {} do not exist for relationship {} of node {}", operation.getPropertyName(), operation.getRelationshipName(),
                operation.getNodeName());

        AbstractPropertyValue defaultPropertyValue = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(relationshipPropertyDefinition);
        relationshipTemplate.getProperties().put(operation.getPropertyName(), defaultPropertyValue);

        log.debug("Remove association from property <{}> of relationship template <{}> of node <{}> to an input of the topology <{}>.",
                operation.getPropertyName(), operation.getRelationshipName(), operation.getNodeName(), EditionContextManager.getTopology().getId());
    }
}
