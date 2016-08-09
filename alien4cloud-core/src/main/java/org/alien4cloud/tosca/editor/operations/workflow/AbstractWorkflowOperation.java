package org.alien4cloud.tosca.editor.operations.workflow;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation on workflow
 */
@Getter
@Setter
public abstract class AbstractWorkflowOperation extends AbstractEditorOperation {
    @NotBlank
    private String workflowName;
}
