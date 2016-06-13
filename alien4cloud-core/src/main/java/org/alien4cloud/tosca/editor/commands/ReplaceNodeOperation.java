package org.alien4cloud.tosca.editor.commands;

import lombok.Getter;
import lombok.Setter;

/**
 * Replace a node template with a compatible node template (inherited or one that fulfill the same used capabilities and requirements etc.).
 */
@Getter
@Setter
public class ReplaceNodeOperation extends AbstractEditorOperation {
    private String nodeTemplateName;
    /** Id of the new indexed node type to assign to the node. */
    private String newTypeId;
}
