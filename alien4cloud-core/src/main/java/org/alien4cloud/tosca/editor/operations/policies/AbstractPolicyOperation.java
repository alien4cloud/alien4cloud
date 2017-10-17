package org.alien4cloud.tosca.editor.operations.policies;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Abstract operation for policy manipulation.
 */
@Getter
@Setter
public abstract class AbstractPolicyOperation extends AbstractEditorOperation {
    @NotBlank
    private String policyName;
}