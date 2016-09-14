package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.ConnectStepToOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.paas.wf.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link ConnectStepToOperation} operation
 */
@Slf4j
@Component
public class ConnectStepToProcessor extends AbstractWorkflowProcessor<ConnectStepToOperation> {

    @Override
    protected void processWorkflowOperation(ConnectStepToOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("connecting step <{}> to <{}> in the workflow <{}> from topology <{}>", operation.getFromStepId(),
                StringUtils.join(operation.getToStepIds(), ","), workflow.getName(), topology.getId());
        workflowBuilderService.connectStepTo(topology, workflow.getName(), operation.getFromStepId(), operation.getToStepIds());
    }
}
