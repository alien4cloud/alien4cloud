package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.IFileRepository;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;

/**
 * .
 */
public class UpdateNodeDeploymentArtifactProcessor implements IEditorOperationProcessor<UpdateNodeDeploymentArtifactOperation> {
    @Resource
    private IFileRepository artifactRepository;

    @Override
    public void process(UpdateNodeDeploymentArtifactOperation operation) {
        // FIXME
        // Perform check that authorization's ok
        Topology topology = TopologyEditionContextManager.getTopology();

        // Get the node template's artifacts to update
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        DeploymentArtifact artifact = nodeTemplate.getArtifacts() == null ? null : nodeTemplate.getArtifacts().get(operation.getArtifactName());
        if (artifact == null) {
            throw new NotFoundException("Artifact with key [" + operation.getArtifactName() + "] do not exist");
        }
        String oldArtifactId = artifact.getArtifactRef();
        // FIXME when using git we should not have artifat repository anymore in alien (all should be part of archive).
        if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
            artifactRepository.deleteFile(oldArtifactId);
        }

        // InputStream artifactStream = artifactFile.getInputStream();
        // try {
        // String artifactFileId = artifactRepository.storeFile(artifactStream);
        // artifact.setArtifactName(artifactFile.getOriginalFilename());
        // artifact.setArtifactRef(artifactFileId);
        // artifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY);
        // topologyServiceCore.save(topology);
        // return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
        // } finally {
        // Closeables.close(artifactStream, true);
        // }
    }
}