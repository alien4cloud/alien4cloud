package org.alien4cloud.tosca.editor.operations;

import org.alien4cloud.tosca.model.CSARDependency;
import alien4cloud.utils.AlienUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * Recover the topology when dependencies have changed.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecoverTopologyOperation extends AbstractEditorOperation {

    /**
     * dependencies that have changed since last added in a given topology
     */
    Set<CSARDependency> updatedDependencies;

    /**
     * List of the operations generated to recover the topology
     *
     */
    List<AbstractEditorOperation> recoveringOperations;


    @Override
    @JsonProperty(value = "resume", access = JsonProperty.Access.READ_ONLY)
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