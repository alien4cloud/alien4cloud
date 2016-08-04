package org.alien4cloud.tosca.editor.processors.nodetemplate.outputs;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.MapUtil;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodeCapabilityPropertyAsOutputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

/**
 * Set a given property of a node's capability as output for the topology.
 */
@Slf4j
@Component
public class UnSetNodeCapabilityPropertyAsOutputProcessor extends AbstractNodeProcessor<UnSetNodeCapabilityPropertyAsOutputOperation> {

    @Override
    protected void processNodeOperation(UnSetNodeCapabilityPropertyAsOutputOperation operation, NodeTemplate nodeTemplate) {

        Topology topology = EditionContextManager.getTopology();

        // check if the capability / property exists
        check(operation, topology, nodeTemplate);

        Map<String, Map<String, Set<String>>> outputs = topology.getOutputCapabilityProperties();
        removeAndClean(operation, outputs);
        topology.setOutputCapabilityProperties(outputs);

        log.debug("Set node<{}> capability <{}>'s property <{}> as output for the topology <{}>.", operation.getNodeName(), operation.getCapabilityName(),
                operation.getPropertyName(), topology.getId());
    }

    private void removeAndClean(UnSetNodeCapabilityPropertyAsOutputOperation operation, Map<String, Map<String, Set<String>>> outputs) {
        outputs.get(operation.getNodeName()).get(operation.getCapabilityName()).remove(operation.getPropertyName());
        if (CollectionUtils.isEmpty(outputs.get(operation.getNodeName()).get(operation.getCapabilityName()))) {
            outputs.get(operation.getNodeName()).remove(operation.getCapabilityName());
        }
        if (MapUtils.isEmpty(outputs.get(operation.getNodeName()))) {
            outputs.remove(operation.getNodeName());
        }
    }

    @SuppressWarnings("unchecked")
    private void check(UnSetNodeCapabilityPropertyAsOutputOperation operation, Topology topology, NodeTemplate nodeTemplate) {
        if (nodeTemplate.getCapabilities() == null || nodeTemplate.getCapabilities().get(operation.getCapabilityName()) == null) {
            throw new NotFoundException("Capability " + operation.getCapabilityName() + " not found in node template " + operation.getNodeName());
        }

        Capability capabilityTemplate = nodeTemplate.getCapabilities().get(operation.getCapabilityName());
        IndexedCapabilityType indexedCapabilityType = EditionContextManager.get().getToscaContext().getElement(IndexedCapabilityType.class,
                capabilityTemplate.getType(), true);
        if (indexedCapabilityType.getProperties() == null || !indexedCapabilityType.getProperties().containsKey(operation.getPropertyName())) {
            throw new NotFoundException("Property " + operation.getPropertyName() + " not found in capability " + operation.getCapabilityName() + " of node "
                    + operation.getNodeName());
        }

        Set<String> values = (Set<String>) MapUtil.get(topology.getOutputCapabilityProperties(),
                operation.getNodeName().concat(".").concat(operation.getCapabilityName()));

        if (!AlienUtils.safe(values).contains(operation.getPropertyName())) {
            throw new NotFoundException("Node " + operation.getNodeName() + " capability " + operation.getCapabilityName() + " 's property "
                    + operation.getPropertyName() + " not found in outputs");
        }
    }

}