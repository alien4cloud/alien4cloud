package org.alien4cloud.tosca.editor.processors.relationshiptemplate;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.PropertyValueException;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.UpdateRelationshipPropertyValueOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.properties.constraints.exception.ConstraintFunctionalException;
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

        String propertyName = operation.getPropertyName();
        Object propertyValue = operation.getPropertyValue();

        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        // FIXME we should have the same kind of utility methods to get relationships as we have for nodes.
        RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(operation.getRelationshipName());

        RelationshipType relationshipType = ToscaContext.getOrFail(RelationshipType.class, relationshipTemplate.getType());
        if (!relationshipType.getProperties().containsKey(propertyName)) {
            throw new NotFoundException(
                    "Property <" + propertyName + "> doesn't exists for node <" + operation.getNodeName() + "> of type <" + relationshipType + ">");
        }

        log.debug("Updating property <{}> of the relationship <{}> for the Node template <{}> from the topology <{}>: changing value from [{}] to [{}].",
                propertyName, relationshipType, operation.getNodeName(), topology.getId(), relationshipType.getProperties().get(propertyName), propertyValue);
        try {
            propertyService.setPropertyValue(relationshipTemplate.getProperties(), relationshipType.getProperties().get(propertyName), propertyName,
                    propertyValue);
        } catch (ConstraintFunctionalException e) {
            throw new PropertyValueException(
                    "Error when setting relationship " + operation.getNodeName() + "." + operation.getRelationshipName() + " property.", e, propertyName,
                    propertyValue);
        }
    }
}
