package org.alien4cloud.tosca.model.workflow.activities;

import lombok.Getter;
import lombok.Setter;

/**
 * Call an operation defined on a TOSCA interface of a node, relationship or group.
 */
@Getter
@Setter
public class CallOperationWorkflowActivity extends AbstractWorkflowActivity {
    private String interfaceName;
    private String operationName;

    @Override
    public String getRepresentation() {
        return operationName;
    }
}
