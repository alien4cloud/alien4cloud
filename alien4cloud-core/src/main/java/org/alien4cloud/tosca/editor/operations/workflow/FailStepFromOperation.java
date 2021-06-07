package org.alien4cloud.tosca.editor.operations.workflow;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class FailStepFromOperation extends AbstractWorkflowOperation {
    @NotBlank
    private String toStepId;

    private String[] fromStepIds;

    @Override
    public String commitMessage() {
        return "Add onFailure links from steps <" + StringUtils.join(getFromStepIds(), ",") + "> to step <" + getToStepId() + "> in the workflow <" + getWorkflowName() + ">";
    }

}
