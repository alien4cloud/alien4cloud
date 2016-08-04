package org.alien4cloud.tosca.editor.processors.nodetemplate.outputs;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.MapUtil;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodePropertyAsOutputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.springframework.stereotype.Component;

/**
 * Unset a given property of a node as output for the topology.
 */
@Slf4j
@Component
public class UnSetNodePropertyAsOutputProcessor extends AbstractNodeProcessor<UnSetNodePropertyAsOutputOperation> {

    @Override
    protected void processNodeOperation(UnSetNodePropertyAsOutputOperation operation, NodeTemplate nodeTemplate) {

        Topology topology = EditionContextManager.getTopology();

        // check if the property exists
        check(operation, topology, nodeTemplate);
        Map<String, Set<String>> outputs = topology.getOutputProperties();
        removeAndClean(operation, outputs);
        topology.setOutputProperties(outputs);

        log.debug("Unset node <{}>'s property <{}> as output for the topology <{}>.", operation.getNodeName(), operation.getPropertyName(), topology.getId());
    }

    private void removeAndClean(UnSetNodePropertyAsOutputOperation operation, Map<String, Set<String>> outputs) {
        outputs.get(operation.getNodeName()).remove(operation.getPropertyName());
        if (outputs.get(operation.getNodeName()).isEmpty()) {
            outputs.remove(operation.getNodeName());
        }
    }

    @SuppressWarnings("unchecked")
    private void check(UnSetNodePropertyAsOutputOperation operation, Topology topology, NodeTemplate nodeTemplate) {
        Map<String, AbstractPropertyValue> properties = nodeTemplate.getProperties();
        if (!AlienUtils.safe(properties).containsKey(operation.getPropertyName())) {
            throw new NotFoundException("Property " + operation.getPropertyName() + "not found in node template " + operation.getNodeName() + ".");
        }

        Set<String> values = (Set<String>) MapUtil.get(topology.getOutputProperties(), operation.getNodeName());
        if (!AlienUtils.safe(values).contains(operation.getPropertyName())) {
            throw new NotFoundException("Node " + operation.getNodeName() + " 's property " + operation.getPropertyName() + " not found in outputs");
        }
    }

}