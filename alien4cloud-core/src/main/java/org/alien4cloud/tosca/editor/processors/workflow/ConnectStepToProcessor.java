package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.ConnectStepToOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link ConnectStepToOperation} operation
 */
@Slf4j
@Component
public class ConnectStepToProcessor extends AbstractWorkflowProcessor<ConnectStepToOperation> {

    @Override
    protected void processWorkflowOperation(Csar csar, Topology topology, ConnectStepToOperation operation, Workflow workflow) {
        log.debug("connecting step [ {} ] to [ {} ] in the workflow [ {} ] from topology [ {} ]", operation.getFromStepId(),
                StringUtils.join(operation.getToStepIds(), ","), workflow.getName(), topology.getId());
        workflowBuilderService.connectStepTo(topology, csar, workflow.getName(), operation.getFromStepId(), operation.getToStepIds());
    }
}
