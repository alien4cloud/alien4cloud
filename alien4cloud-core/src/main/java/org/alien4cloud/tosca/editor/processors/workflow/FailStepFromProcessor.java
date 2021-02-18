package org.alien4cloud.tosca.editor.processors.workflow;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.workflow.FailStepFromOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Process the {@link FailStepFromOperation} operation
 */
@Slf4j
@Component
public class FailStepFromProcessor  extends AbstractWorkflowProcessor<FailStepFromOperation> {
    @Override
    protected void processWorkflowOperation(Csar csar, Topology topology, FailStepFromOperation operation, Workflow workflow) {
        log.debug("connecting with onFailure steps [ {} ] to [ {} ] in the workflow [ {} ] from topology [ {} ]", StringUtils.join(operation.getFromStepIds(), ","),
                operation.getToStepId(), workflow.getName(), topology.getId());
        workflowBuilderService.failStepFrom(topology, csar, workflow.getName(), operation.getToStepId(), operation.getFromStepIds());
    }
}
