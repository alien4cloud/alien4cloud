package org.alien4cloud.tosca.editor.processors;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.exception.PropertyValueException;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.properties.constraints.exception.ConstraintFunctionalException;
import alien4cloud.utils.services.PropertyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an update node property value operation against the topology in the edition context.
 */
@Slf4j
@Component
public class UpdateNodePropertyValueProcessor implements IEditorOperationProcessor<UpdateNodePropertyValueOperation> {
    @Resource
    private PropertyService propertyService;

    @Override
    public void process(UpdateNodePropertyValueOperation operation) {
        Topology topology = TopologyEditionContextManager.getTopology();

        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemp = TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        String propertyName = operation.getPropertyName();
        Object propertyValue = operation.getPropertyValue();

        IndexedNodeType node = ToscaContext.getOrFail(IndexedNodeType.class, nodeTemp.getType());

        PropertyDefinition propertyDefinition = node.getProperties().get(propertyName);
        if (propertyDefinition == null) {
            throw new NotFoundException(
                    "Property <" + propertyName + "> doesn't exists for node <" + operation.getNodeName() + "> of type <" + nodeTemp.getType() + ">");
        }

        log.debug("Updating property <{}> of the Node template <{}> from the topology <{}>: changing value from [{}] to [{}].", propertyName,
                operation.getNodeName(), topology.getId(), nodeTemp.getProperties().get(propertyName), propertyValue);

        try {
            propertyService.setPropertyValue(nodeTemp, propertyDefinition, propertyName, propertyValue);
        } catch (ConstraintFunctionalException e) {
            throw new PropertyValueException("Error when setting node " + operation.getNodeName() + " property.", e, propertyName, propertyValue);
        }
    }
}