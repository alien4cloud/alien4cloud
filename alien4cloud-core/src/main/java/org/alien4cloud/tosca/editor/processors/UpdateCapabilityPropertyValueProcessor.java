package org.alien4cloud.tosca.editor.processors;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.services.PropertyService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an update capability property value operation against the topology in the edition context.
 */
@Slf4j
public class UpdateCapabilityPropertyValueProcessor implements IEditorOperationProcessor<UpdateCapabilityPropertyValueOperation> {
    @Resource
    private PropertyService propertyService;

    @Override
    @SneakyThrows
    public void process(UpdateCapabilityPropertyValueOperation operation) {
        Topology topology = TopologyEditionContextManager.getTopology();

        String propertyName = operation.getPropertyName();
        Object propertyValue = operation.getPropertyValue();
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        Capability capability = nodeTemplate.getCapabilities().get(operation.getCapabilityName());

        IndexedCapabilityType capabilityType = ToscaContext.get(IndexedCapabilityType.class, capability.getType());

        if (!capabilityType.getProperties().containsKey(propertyName)) {
            throw new NotFoundException(
                    "Property <" + propertyName + "> doesn't exists for node <" + operation.getNodeName() + "> of type <" + capabilityType + ">");
        }

        log.debug("Updating property <{}> of the capability <{}> for the Node template <{}> from the topology <{}>: changing value from [{}] to [{}].",
                propertyName, capability.getType(), operation.getNodeName(), topology.getId(), capabilityType.getProperties().get(propertyName), propertyValue);

        propertyService.setPropertyValue(capability.getProperties(), capabilityType.getProperties().get(propertyName), propertyName, propertyValue);
    }
}
