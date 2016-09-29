package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.PropertyValueException;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.properties.constraints.exception.ConstraintFunctionalException;
import alien4cloud.utils.services.PropertyService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an update capability property value operation against the topology in the edition context.
 */
@Slf4j
@Component
public class UpdateCapabilityPropertyValueProcessor implements IEditorOperationProcessor<UpdateCapabilityPropertyValueOperation> {
    @Resource
    private PropertyService propertyService;

    @Override
    @SneakyThrows
    public void process(UpdateCapabilityPropertyValueOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        String propertyName = operation.getPropertyName();
        Object propertyValue = operation.getPropertyValue();
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        Capability capability = nodeTemplate.getCapabilities().get(operation.getCapabilityName());

        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capability.getType());

        if (!capabilityType.getProperties().containsKey(propertyName)) {
            throw new NotFoundException(
                    "Property <" + propertyName + "> doesn't exists for node <" + operation.getNodeName() + "> of type <" + capabilityType + ">");
        }

        log.debug("Updating property <{}> of the capability <{}> for the Node template <{}> from the topology <{}>: changing value from [{}] to [{}].",
                propertyName, capability.getType(), operation.getNodeName(), topology.getId(), capabilityType.getProperties().get(propertyName), propertyValue);

        try {
            propertyService.setCapabilityPropertyValue(capability, capabilityType.getProperties().get(propertyName), propertyName, propertyValue);
        } catch (ConstraintFunctionalException e) {
            throw new PropertyValueException("Error when setting node " + operation.getNodeName() + " property.", e, propertyName, propertyValue);
        }
    }
}
