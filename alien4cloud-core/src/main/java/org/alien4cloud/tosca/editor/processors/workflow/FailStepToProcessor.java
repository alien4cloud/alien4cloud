package org.alien4cloud.tosca.editor.processors.workflow;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.workflow.FailStepToOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Process the {@link FailStepToOperation} operation
 */
@Slf4j
@Component
public class FailStepToProcessor extends AbstractWorkflowProcessor<FailStepToOperation>  {
    @Override
    protected void processWorkflowOperation(Csar csar, Topology topology, FailStepToOperation operation, Workflow workflow) {
        log.debug("Connecting with onFailure link step [ {} ] to [ {} ] in the workflow [ {} ] from topology [ {} ]", operation.getFromStepId(),
                StringUtils.join(operation.getToStepIds(), ","), workflow.getName(), topology.getId());
        workflowBuilderService.failStepTo(topology, csar, workflow.getName(), operation.getFromStepId(), operation.getToStepIds());
    }
}
