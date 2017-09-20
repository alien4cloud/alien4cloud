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

    public void setOperationFqn(String operationFqn) {
        int lastDotIdx = operationFqn.lastIndexOf(".");
        if (lastDotIdx > 0) {
            this.interfaceName = operationFqn.substring(0, lastDotIdx);
            this.operationName = operationFqn.substring(lastDotIdx + 1, operationFqn.length());
        }
    }

    @Override
    public String getRepresentation() {
        return operationName;
    }
}
