package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.TreeNode;

/**
 * Process an {@link UpdateNodeDeploymentArtifactOperation}.
 */
@Component
public class UpdateNodeDeploymentArtifactProcessor implements IEditorOperationProcessor<UpdateNodeDeploymentArtifactOperation> {
    @Override
    public void process(UpdateNodeDeploymentArtifactOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        // Get the node template's artifacts to update
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        DeploymentArtifact artifact = nodeTemplate.getArtifacts() == null ? null : nodeTemplate.getArtifacts().get(operation.getArtifactName());
        if (artifact == null) {
            throw new NotFoundException("Artifact with key [" + operation.getArtifactName() + "] do not exist");
        }

        if (operation.getArtifactRepository() == null) {
            // this is an archive file, ensure that the file exists within the archive
            TreeNode root = EditionContextManager.get().getArchiveContentTree().getChildren().first();
            TreeNode target = root;
            String[] pathElements = operation.getArtifactReference().split("/");
            for (int i = 0; i < pathElements.length; i++) {
                String pathElement = pathElements[i];
                TreeNode child = target.getChild(pathElement);
                if (child == null) {
                    throw new NotFoundException(
                            "The artifact specified at path <" + operation.getArtifactReference() + "> does not exists in the topology archive.");
                }
                target = child;
            }
        } else {
            // FIXME ensure that the repository is defined in the topology or globally in a4c
            throw new NotImplementedException("Alien 4 Cloud doesn't support repositories in topology editor.");
        }

        artifact.setArtifactRef(operation.getArtifactReference());
        artifact.setArtifactRepository(operation.getArtifactRepository());
    }
}