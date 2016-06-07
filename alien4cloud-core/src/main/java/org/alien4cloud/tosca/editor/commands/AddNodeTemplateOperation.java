package org.alien4cloud.tosca.editor.commands;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to add a new node template.
 */
@Getter
@Setter
public class AddNodeTemplateOperation implements IEditorOperation {
    private String message;
}
