package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to rebuild a node template.
 *
 */
@Getter
@Setter
public class  RebuildNodeOperation extends AbstractNodeOperation {
    @Override
    public String commitMessage() {
        return "rebuild node <" + getNodeName() + ">";
    }
}