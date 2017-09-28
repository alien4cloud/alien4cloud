package org.alien4cloud.tosca.editor.processors.nodetemplate;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.topology.TemplateBuilder;
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
        NodeType type = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
        NodeTemplate rebuiltNodeTemplate = TemplateBuilder.buildNodeTemplate(type, nodeTemplate);
        rebuiltNodeTemplate.setName(operation.getNodeName());
        topology.getNodeTemplates().put(operation.getNodeName(), rebuiltNodeTemplate);
    }
}