package org.alien4cloud.tosca.editor.operations;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the name of a node template.
 */
@Getter
@Setter
public class RenameNodeOperation extends AbstractNodeOperation {
    @NotBlank
    private String newName;
}
