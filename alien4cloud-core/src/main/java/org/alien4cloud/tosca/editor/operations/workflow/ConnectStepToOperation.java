package org.alien4cloud.tosca.editor.operations.workflow;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to connect a given step to a set of step.
 */
@Getter
@Setter
public class ConnectStepToOperation extends AbstractWorkflowOperation {
    @NotBlank
    private String fromStepId;

    private String[] toStepIds;

    @Override
    public String commitMessage() {
        return "Connect step <" + getFromStepId() + "> to steps <" + StringUtils.join(getToStepIds(), ",") + "> in the workflow <" + getWorkflowName() + ">";
    }
}
