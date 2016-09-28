package org.alien4cloud.tosca.editor.processors.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.inputs.DeleteInputArtifactOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.utils.InputArtifactUtil;

/**
 * Delete an input artifact.
 */
@Component
public class DeleteInputArtifactProcessor implements IEditorOperationProcessor<DeleteInputArtifactOperation> {
    @Override
    public void process(DeleteInputArtifactOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (!safe(topology.getInputArtifacts()).containsKey(operation.getInputName())) {
            throw new NotFoundException("Input artifact <" + operation.getInputName() + "> can not be found in the topology.");
        }

        DeploymentArtifact inputArtifact = topology.getInputArtifacts().remove(operation.getInputName());
        // change the value of concerned node template artifacts
        for (NodeTemplate nodeTemplate : safe(topology.getNodeTemplates()).values()) {
            for (DeploymentArtifact dArtifact : safe(nodeTemplate.getArtifacts()).values()) {
                if (operation.getInputName().equals(InputArtifactUtil.getInputArtifactId(dArtifact))) {
                    InputArtifactUtil.unsetInputArtifact(dArtifact);
                }
            }
        }
    }
}