package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Operation on a node template.
 */
@Getter
@Setter
public abstract class AbstractNodeOperation extends AbstractEditorOperation {
    @NotBlank
    private String nodeName;
}