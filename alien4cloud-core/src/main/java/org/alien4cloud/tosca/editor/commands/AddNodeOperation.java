package org.alien4cloud.tosca.editor.commands;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Operation to add a new node template.
 */
@Getter
@Setter
public class AddNodeOperation extends AbstractEditorOperation {
    /** the name of the node template */
    @NotBlank
    private String name;
    /** related NodeType id */
    @NotBlank
    private String indexedNodeTypeId;
}