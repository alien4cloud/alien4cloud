package org.alien4cloud.tosca.editor.processors.workflow;

import alien4cloud.exception.AlreadyExistException;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RenameWorkflowOperation;
import org.springframework.stereotype.Component;

import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link RenameWorkflowOperation} operation
 * Renames a workflow
 */
@Slf4j
@Component
public class RenameWorkflowProcessor extends AbstractWorkflowProcessor<RenameWorkflowOperation> {

    @Override
    protected void processWorkflowOperation(RenameWorkflowOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        ensureNotStandard(workflow, "standard workflow <" + workflow.getName() + "> can not be renamed");
        ensureUniqueness(topology, operation.getNewName());
        log.debug("renaming workflow <{}> to <{}> from topology <{}>", workflow.getName(), operation.getNewName(), topology.getId());
        topology.getWorkflows().remove(workflow.getName());
        workflow.setName(operation.getNewName());
        topology.getWorkflows().put(workflow.getName(), workflow);
    }

    private void ensureUniqueness(Topology topology, String name) {
        if (topology.getWorkflows().containsKey(name)) {
            throw new AlreadyExistException(String.format("The workflow '%s' already exists in topology '%s'.", name, topology.getId()));
        }
    }
}
