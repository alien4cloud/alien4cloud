package org.alien4cloud.tosca.editor.processors.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.inputs.RenameInputArtifactOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.utils.InputArtifactUtil;
import org.springframework.stereotype.Component;

/**
 * Rename an input artifact.
 */
@Component
public class RenameInputArtifactProcessor implements IEditorOperationProcessor<RenameInputArtifactOperation> {
    @Override
    public void process(RenameInputArtifactOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (operation.getNewInputName() == null || operation.getNewInputName().isEmpty() || !operation.getNewInputName().matches("\\w+")) {
            throw new InvalidNameException("newInputName", operation.getNewInputName(), "\\w+");
        }
        if (safe(topology.getInputArtifacts()).containsKey(operation.getNewInputName())) {
            throw new AlreadyExistException("Input artifact with name <" + operation.getNewInputName() + "> already exists.");
        }
        if (!safe(topology.getInputArtifacts()).containsKey(operation.getInputName())) {
            throw new NotFoundException("Input artifact with name <" + operation.getInputName() + "> does not exists.");
        }
        DeploymentArtifact inputArtifact = topology.getInputArtifacts().remove(operation.getInputName());
        topology.getInputArtifacts().put(operation.getNewInputName(), inputArtifact);
        // change the value of concerned node template artifacts
        for (NodeTemplate nodeTemplate : safe(topology.getNodeTemplates()).values()) {
            for (DeploymentArtifact dArtifact : safe(nodeTemplate.getArtifacts()).values()) {
                InputArtifactUtil.updateInputArtifactIdIfNeeded(dArtifact, operation.getInputName(), operation.getNewInputName());
            }
        }
    }
}
