package org.alien4cloud.tosca.editor.processors.nodetemplate;

import alien4cloud.utils.AlienUtils;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.topology.TopologyUtils;

import org.alien4cloud.tosca.editor.EditionContextManager;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;

import java.util.Map;

/**
 * Abstract operation to get a required node template.
 */
public abstract class AbstractNodeProcessor<T extends AbstractNodeOperation> implements IEditorOperationProcessor<T> {
    @Override
    public void process(T operation) {
        Topology topology = EditionContextManager.getTopology();
        NodeTemplate nodeTemplate = AlienUtils.getOrFail(topology.getNodeTemplates(), operation.getNodeName(),
                "The node with name [ {} ] cannot be found in the topology [ {} ].", operation.getNodeName(), topology.getId());
        processNodeOperation(operation, nodeTemplate);
    }

    protected abstract void processNodeOperation(T operation, NodeTemplate nodeTemplate);
}
