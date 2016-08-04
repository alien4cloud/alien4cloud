package org.alien4cloud.tosca.editor.processors.nodetemplate.outputs;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.utils.AlienUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodePropertyAsOutputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.springframework.stereotype.Component;

/**
 * Set a given property of a node as output for the topology.
 */
@Slf4j
@Component
public class SetNodePropertyAsOutputProcessor extends AbstractNodeProcessor<SetNodePropertyAsOutputOperation> {

    @Override
    protected void processNodeOperation(SetNodePropertyAsOutputOperation operation, NodeTemplate nodeTemplate) {

        Topology topology = EditionContextManager.getTopology();

        // check if the property exists
        Map<String, AbstractPropertyValue> properties = nodeTemplate.getProperties();
        if (!AlienUtils.safe(properties).containsKey(operation.getPropertyName())) {
            throw new NotFoundException("Property " + operation.getPropertyName() + "not found in node template " + operation.getNodeName() + ".");
        }

        Map<String, Set<String>> outputs = topology.getOutputProperties();
        if (outputs == null) {
            outputs = Maps.newHashMap();
        }

        if (outputs.containsKey(operation.getNodeName())) {
            outputs.get(operation.getNodeName()).add(operation.getPropertyName());
        } else {
            outputs.put(operation.getNodeName(), Sets.newHashSet(operation.getPropertyName()));
        }

        topology.setOutputProperties(outputs);

        log.debug("Set node <{}>'s property <{}> as output for the topology <{}>.", operation.getNodeName(), operation.getPropertyName(), topology.getId());
    }

}