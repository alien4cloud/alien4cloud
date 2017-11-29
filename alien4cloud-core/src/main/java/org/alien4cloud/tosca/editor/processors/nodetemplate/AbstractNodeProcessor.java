package org.alien4cloud.tosca.editor.processors.nodetemplate;

import alien4cloud.utils.AlienUtils;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;

/**
 * Abstract operation to get a required node template.
 */
public abstract class AbstractNodeProcessor<T extends AbstractNodeOperation> implements IEditorOperationProcessor<T> {
    @Override
    public void process(Csar csar, Topology topology, T operation) {
        NodeTemplate nodeTemplate = AlienUtils.getOrFail(topology.getNodeTemplates(), operation.getNodeName(),
                "The node with name [ {} ] cannot be found in the topology [ {} ].", operation.getNodeName(), topology.getId());
        processNodeOperation(csar, topology, operation, nodeTemplate);
    }

    protected abstract void processNodeOperation(Csar csar, Topology topology, T operation, NodeTemplate nodeTemplate);
}
