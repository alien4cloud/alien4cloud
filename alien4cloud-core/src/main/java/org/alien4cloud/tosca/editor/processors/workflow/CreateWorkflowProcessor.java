package org.alien4cloud.tosca.editor.processors.workflow;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.util.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link CreateWorkflowOperation} operation.
 */
@Slf4j
@Component
public class CreateWorkflowProcessor implements IEditorOperationProcessor<CreateWorkflowOperation> {

    @Inject
    private WorkflowsBuilderService workflowsBuilderService;

    @Override
    public void process(CreateWorkflowOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (log.isDebugEnabled()) {
            log.debug("creating new workflow [ {} ] in topology [ {} ]", operation.getWorkflowName(), topology.getId());
        }
        WorkflowUtils.validateName(operation.getWorkflowName());
        workflowsBuilderService.createWorkflow(topology, operation.getWorkflowName());
    }

}
