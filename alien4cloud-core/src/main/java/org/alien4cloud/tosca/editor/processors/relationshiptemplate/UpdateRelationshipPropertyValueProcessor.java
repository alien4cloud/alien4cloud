package org.alien4cloud.tosca.editor.processors.relationshiptemplate;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.PropertyValueException;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.UpdateRelationshipPropertyValueOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.exceptions.ConstraintFunctionalException;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.services.PropertyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an update relationship property value operation against the topology in the edition context.
 */
@Slf4j
@Component
public class UpdateRelationshipPropertyValueProcessor implements IEditorOperationProcessor<UpdateRelationshipPropertyValueOperation> {
    @Resource
    private PropertyService propertyService;

    @Override
    public void process(UpdateRelationshipPropertyValueOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        NodeTemplate nodeTemplate = AlienUtils.getOrFail(topology.getNodeTemplates(), operation.getNodeName(),
                "The node with name <{}> cannot be found in the topology.", operation.getNodeName());
        RelationshipTemplate relationshipTemplate = AlienUtils.getOrFail(nodeTemplate.getRelationships(), operation.getRelationshipName(),
                "The relationship with name <{}> cannot be found in the node <{}>.", operation.getRelationshipName(), operation.getNodeName());

        RelationshipType relationshipType = ToscaContext.getOrFail(RelationshipType.class, relationshipTemplate.getType());
        PropertyDefinition propertyDefinition = AlienUtils.getOrFail(relationshipType.getProperties(), operation.getPropertyName(),
                "Property <{}> doesn't exists in type <{}> for relationship <{}> of node <{}>.", operation.getPropertyName(), relationshipTemplate.getType(),
                operation.getRelationshipName(), operation.getNodeName());

        log.debug("Updating property <{}> of the relationship <{}> for the Node template <{}> from the topology <{}>: changing value from [{}] to [{}].",
                operation.getPropertyName(), relationshipType, operation.getNodeName(), topology.getId(),
                relationshipType.getProperties().get(operation.getPropertyName()), operation.getPropertyValue());
        try {
            propertyService.setPropertyValue(relationshipTemplate, propertyDefinition, operation.getPropertyName(), operation.getPropertyValue());
        } catch (ConstraintFunctionalException e) {
            throw new PropertyValueException(
                    "Error when setting relationship " + operation.getNodeName() + "." + operation.getRelationshipName() + " property.", e,
                    operation.getPropertyName(), operation.getPropertyValue());
        }
    }
}
