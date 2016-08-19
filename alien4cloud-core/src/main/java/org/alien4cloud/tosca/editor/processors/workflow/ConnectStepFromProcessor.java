package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.ConnectStepFromOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link ConnectStepFromOperation} operation
 */
@Slf4j
@Component
public class ConnectStepFromProcessor extends AbstractWorkflowProcessor<ConnectStepFromOperation> {

    @Override
    protected void processWorkflowOperation(ConnectStepFromOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("connecting steps <{}> to <{}> in the workflow <{}> from topology <{}>", StringUtils.join(operation.getFromStepIds(), ","),
                operation.getToStepId(), workflow.getName(), topology.getId());
        workflowBuilderService.connectStepFrom(topology, workflow.getName(), operation.getToStepId(), operation.getFromStepIds());
    }
}
