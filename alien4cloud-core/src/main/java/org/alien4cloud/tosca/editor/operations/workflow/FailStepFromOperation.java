package org.alien4cloud.tosca.editor.operations.workflow;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class FailStepFromOperation extends AbstractWorkflowOperation {
    @NotBlank
    private String fromStepId;

    private String[] toStepIds;

    @Override
    public String commitMessage() {
        return "Add onFailure link from step <" + getFromStepId() + "> to steps <" + StringUtils.join(getToStepIds(), ",") + "> in the workflow <" + getWorkflowName() + ">";
    }
}
