package org.alien4cloud.tosca.editor.commands;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Operation on a node template.
 */
@Getter
@Setter
public class AbstractNodeOperation extends AbstractEditorOperation {
    @NotBlank
    private String nodeName;
}