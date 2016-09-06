package org.alien4cloud.tosca.editor.operations;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reset (empty) a topology.
 */
@Getter
@Setter
@NoArgsConstructor
public class ResetTopologyOperation extends AbstractEditorOperation {

    @Override
    public String commitMessage() {
        return "Reset the topology to blank.";
    }
}