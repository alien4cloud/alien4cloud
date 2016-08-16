package org.alien4cloud.tosca.editor.processors.nodetemplate.outputs;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.utils.AlienUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * Set a given property of a node's capability as output for the topology.
 */
@Slf4j
@Component
public class SetNodeCapabilityPropertyAsOutputProcessor extends AbstractNodeProcessor<SetNodeCapabilityPropertyAsOutputOperation> {

    @Override
    protected void processNodeOperation(SetNodeCapabilityPropertyAsOutputOperation operation, NodeTemplate nodeTemplate) {

        Topology topology = EditionContextManager.getTopology();

        // check if the capability / property exists
        check(operation, topology, nodeTemplate);

        //ensure non null maps and collections
        Map<String, Map<String, Set<String>>> outputs = emptyIfNull(topology.getOutputCapabilityProperties());
        Map<String, Set<String>> capabilitiesOutputs = emptyIfNull(outputs.get(operation.getNodeName()));
        outputs.put(operation.getNodeName(), capabilitiesOutputs);
        Set<String> outputProperties = emptyIfNull(capabilitiesOutputs.get(operation.getCapabilityName()));
        capabilitiesOutputs.put(operation.getCapabilityName(), outputProperties);
        outputProperties.add(operation.getPropertyName());

        topology.setOutputCapabilityProperties(outputs);

        log.debug("Set node<{}> capability <{}>'s property <{}> as output for the topology <{}>.", operation.getNodeName(), operation.getCapabilityName(),
                operation.getPropertyName(), topology.getId());
    }

    private void check(SetNodeCapabilityPropertyAsOutputOperation operation, Topology topology, NodeTemplate nodeTemplate) {
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
    }

    private  <K, V> Map<K, V> emptyIfNull(Map<K, V> map){
        return map==null?Maps.newHashMap():map;
    }
    private  <E> Set<E> emptyIfNull(Set<E> set){
        return set==null?Sets.newHashSet():set;
    }

}