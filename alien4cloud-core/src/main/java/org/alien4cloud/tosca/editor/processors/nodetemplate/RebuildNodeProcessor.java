package org.alien4cloud.tosca.editor.processors.nodetemplate;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.topology.TemplateBuilder;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.RebuildNodeOperation;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Process an {@link RebuildNodeOperation}
 *
 * Rebuild a node template, synching it with the indexedNodeType store at the moment
 */
@Slf4j
@Component
public class RebuildNodeProcessor extends AbstractNodeProcessor<RebuildNodeOperation> {
    @Override
    protected void processNodeOperation(Csar csar, Topology topology, RebuildNodeOperation operation, NodeTemplate nodeTemplate) {
        log.debug("Rebuilding the node template [ {} ] of topology [ {} ] .", operation.getNodeName(), topology.getId());
        NodeType type = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
        // Artifacts are copied from the type to the template
        // In case of an update of version, we must remove old artifacts copied from old types
        // FIXME This is very tricky, we must think about stopping copying artifact from types to templates
        nodeTemplate.getArtifacts().entrySet().removeIf(artifactEntry -> Objects.equals(type.getArchiveName(), artifactEntry.getValue().getArchiveName()));

        // We need to do this on the whole hierarchy
        for (String typeName : type.getDerivedFrom()) {
            NodeType subType = ToscaContext.getOrFail(NodeType.class, typeName);
            nodeTemplate.getArtifacts().entrySet().removeIf(artifactEntry -> Objects.equals(subType.getArchiveName(), artifactEntry.getValue().getArchiveName()));
        }

        NodeTemplate rebuiltNodeTemplate = TemplateBuilder.buildNodeTemplate(type, nodeTemplate);
        rebuiltNodeTemplate.setName(operation.getNodeName());
        topology.getNodeTemplates().put(operation.getNodeName(), rebuiltNodeTemplate);
    }
}