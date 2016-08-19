package org.alien4cloud.tosca.editor.operations.nodetemplate;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to add a new node template.
 */
@Getter
@Setter
public class AddNodeOperation extends AbstractNodeOperation {
    /** related NodeType id */
    @NotBlank
    private String indexedNodeTypeId;

    @Override
    public String commitMessage() {
        return "add node <" + getNodeName() + "> of type <" + indexedNodeTypeId + ">";
    }
}