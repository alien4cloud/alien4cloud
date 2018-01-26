package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.operations.workflow.ReinitializeWorkflowOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link ReinitializeWorkflowOperation} operation
 * Reinitialize a workflow
 */
@Slf4j
@Component
public class ReinitializeWorkflowProcessor extends AbstractWorkflowProcessor<ReinitializeWorkflowOperation> {

    @Override
    protected void processWorkflowOperation(Csar csar, Topology topology, ReinitializeWorkflowOperation operation, Workflow workflow) {
        ensureStandard(workflow, "Non standard workflow <" + workflow.getName() + "> can not be reinitialized");
        log.debug("reinitializing workflow [ {} ] from topology [ {} ]", workflow.getName(), topology.getId());
        workflowBuilderService.reinitWorkflow(workflow.getName(), workflowBuilderService.buildTopologyContext(topology, csar), true);
    }

}
