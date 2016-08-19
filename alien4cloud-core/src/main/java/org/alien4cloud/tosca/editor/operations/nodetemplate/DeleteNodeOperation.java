package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Getter
@Setter
public class DeleteNodeOperation extends AbstractNodeOperation {
    @Override
    public String commitMessage() {
        return "delete node <" + getNodeName() + ">";
    }
}
