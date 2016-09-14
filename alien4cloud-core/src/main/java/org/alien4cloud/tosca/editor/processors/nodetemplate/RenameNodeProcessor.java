package org.alien4cloud.tosca.editor.processors.nodetemplate;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.RenameNodeOperation;

import alien4cloud.exception.InvalidNodeNameException;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyUtils;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

/**
 * Process a {@link RenameNodeOperation}
 */
@Slf4j
@Component
public class RenameNodeProcessor implements IEditorOperationProcessor<RenameNodeOperation> {
    @Resource
    private TopologyService topologyService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    public void process(RenameNodeOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        if (!TopologyUtils.isValidNodeName(operation.getNewName())) {
            throw new InvalidNodeNameException("A name should only contains alphanumeric character from the basic Latin alphabet and the underscore.");
        }
        // ensure there is node templates
        topologyService.isUniqueNodeTemplateName(topology, operation.getNewName());

        TopologyUtils.renameNodeTemplate(topology, operation.getNodeName(), operation.getNewName());
        workflowBuilderService.renameNode(topology, operation.getNodeName(), operation.getNewName());
        log.debug("Renaming the Node template <{}> with <{}> in the topology <{}> .", operation.getNodeName(), operation.getNewName(), topology.getId());
    }
}
