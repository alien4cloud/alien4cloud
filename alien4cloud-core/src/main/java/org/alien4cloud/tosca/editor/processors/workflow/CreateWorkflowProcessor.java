package org.alien4cloud.tosca.editor.processors.workflow;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Process the {@link CreateWorkflowOperation} operation
 */
@Slf4j
@Component
public class CreateWorkflowProcessor implements IEditorOperationProcessor<CreateWorkflowOperation> {

    @Inject
    private WorkflowsBuilderService workflowsBuilderService;

    @Override
    public void process(CreateWorkflowOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        ensureUniqueness(topology, operation.getWorkflowName());
        log.debug("creating new workflow <{}> in topology <{}>", operation.getWorkflowName(), topology.getId());
        Workflow wf = workflowsBuilderService.ceateWorkflow(topology, operation.getWorkflowName());
    }

    private void ensureUniqueness(Topology topology, String name) {
        if (topology.getWorkflows().containsKey(name)) {
            throw new AlreadyExistException(String.format("The workflow '%s' already exists in topology '%s'.", name, topology.getId()));
        }
    }
}
