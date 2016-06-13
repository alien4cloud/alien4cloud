package org.alien4cloud.tosca.editor.commands;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the name of a node template.
 */
@Getter
@Setter
public class RenameNodeOperation extends AbstractEditorOperation {
    @NotBlank
    private String currentName;
    @NotBlank
    private String newName;
}
