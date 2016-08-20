package org.alien4cloud.tosca.editor.operations;

import alien4cloud.utils.AlienUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * Recover the topology when dependencies have changed.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecoverTopologyOperation extends AbstractEditorOperation {
    /**
     * List of the operations generated to recover the topology
     *
     */
    List<AbstractEditorOperation> recoveringOperations;

    @Override
    public String commitMessage() {
        StringBuilder commitMessage = new StringBuilder("Topology recovering: ");
        if (CollectionUtils.isEmpty(recoveringOperations)) {
            commitMessage.append("Nothing to do.");
        } else {
            for (AbstractEditorOperation recoverageOperation : AlienUtils.safe(recoveringOperations)) {
                commitMessage.append("\n\t").append(recoverageOperation.commitMessage());
            }
        }
        return commitMessage.toString();
    }
}