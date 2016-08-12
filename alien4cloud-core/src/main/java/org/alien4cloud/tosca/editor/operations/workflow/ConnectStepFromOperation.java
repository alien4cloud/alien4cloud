package org.alien4cloud.tosca.editor.operations.workflow;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Operation to connect steps from a given set to another step.
 */
@Getter
@Setter
public class ConnectStepFromOperation extends AbstractWorkflowOperation {
    @NotBlank
    private String toStepId;

    private String[] fromStepIds;

    @Override
    public String commitMessage() {
        return "Connect steps <" + StringUtils.join(getFromStepIds(), ",") + "> to step <" + getToStepId() + "> in the workflow <" + getWorkflowName() + ">";
    }
}
