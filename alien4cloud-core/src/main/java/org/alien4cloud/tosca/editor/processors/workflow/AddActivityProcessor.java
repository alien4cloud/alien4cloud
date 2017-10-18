package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.AddActivityOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link AddActivityOperation} operation
 */
@Slf4j
@Component
public class AddActivityProcessor extends AbstractWorkflowProcessor<AddActivityOperation> {

    @Override
    protected void processWorkflowOperation(Csar csar, Topology topology, AddActivityOperation operation, Workflow workflow) {
        log.debug("adding activity [ {} ] to the workflow [ {} ] from topology [ {} ]", operation.getActivity().toString(), workflow.getName(), topology.getId());
        workflowBuilderService.addActivity(topology, csar, operation.getWorkflowName(), operation.getRelatedStepId(),
                operation.isBefore(), operation.getTarget(), operation.getTargetRelationship(), operation.getActivity());
    }
}
