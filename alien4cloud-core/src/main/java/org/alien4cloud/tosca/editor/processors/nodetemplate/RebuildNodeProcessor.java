package org.alien4cloud.tosca.editor.processors.nodetemplate;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.RebuildNodeOperation;
import org.springframework.stereotype.Component;

/**
 * Process an {@link RebuildNodeOperation}
 *
 * Rebuild a node template, synching it with the indexedNodeType store at the moment
 */
@Slf4j
@Component
public class RebuildNodeProcessor extends AbstractNodeProcessor<RebuildNodeOperation> {
    @Override
    protected void processNodeOperation(RebuildNodeOperation operation, NodeTemplate nodeTemplate) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("Rebuilding the node template <{}> of topology <{}> .", operation.getNodeName(), topology.getId());
        IndexedNodeType type = ToscaContext.getOrFail(IndexedNodeType.class, nodeTemplate.getType());
        NodeTemplate rebuiltNodeTemplate = NodeTemplateBuilder.buildNodeTemplate(type, nodeTemplate);
        rebuiltNodeTemplate.setName(operation.getNodeName());
        topology.getNodeTemplates().put(operation.getNodeName(), rebuiltNodeTemplate);
    }
}