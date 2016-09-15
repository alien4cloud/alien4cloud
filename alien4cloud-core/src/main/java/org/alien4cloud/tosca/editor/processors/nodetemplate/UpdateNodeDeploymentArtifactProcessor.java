package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation;
import org.alien4cloud.tosca.editor.processors.FileProcessorHelper;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.topology.TopologyServiceCore;

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
            FileProcessorHelper.getFileTreeNode(operation.getArtifactReference());
            artifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_TOPOLOGY_REPOSITORY);
        } else {
            // FIXME ensure that the repository is defined in the topology or globally in a4c
            throw new NotImplementedException("Alien 4 Cloud doesn't support repositories in topology editor.");
        }
        artifact.setArtifactRef(operation.getArtifactReference());
    }
}