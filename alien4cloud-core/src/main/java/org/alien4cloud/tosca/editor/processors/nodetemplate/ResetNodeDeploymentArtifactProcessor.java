package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.ResetNodeDeploymentArtifactOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyUtils;

/**
 * Process an {@link ResetNodeDeploymentArtifactOperation}.
 */
@Component
public class ResetNodeDeploymentArtifactProcessor implements IEditorOperationProcessor<ResetNodeDeploymentArtifactOperation> {
    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Override
    public void process(Csar csar, Topology topology, ResetNodeDeploymentArtifactOperation operation) {
        // Get the node template's artifacts to reset
        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = TopologyUtils.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        DeploymentArtifact currentArtifact = nodeTemplate.getArtifacts() == null ? null : nodeTemplate.getArtifacts().get(operation.getArtifactName());
        if (currentArtifact == null) {
            throw new NotFoundException(
                    "Artifact with key [" + operation.getArtifactName() + "] do not exist in node template [" + nodeTemplate.getName() + "].");
        }

        // Get the node type's artifact
        Map<String, NodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, false, true);
        NodeType nodeType = nodeTypes.get(nodeTemplate.getType());
        DeploymentArtifact artifactFromNodeType = nodeType.getArtifacts() == null ? null : nodeType.getArtifacts().get(operation.getArtifactName());
        if (artifactFromNodeType == null) {
            throw new NotFoundException("Artifact with key [" + operation.getArtifactName() + "] do not exist in node type [" + nodeType.getId() + "].");
        }

        currentArtifact.setArtifactRef(artifactFromNodeType.getArtifactRef());
        currentArtifact.setArtifactName(artifactFromNodeType.getArtifactName());
        currentArtifact.setArtifactType(artifactFromNodeType.getArtifactType());
        currentArtifact.setArtifactRepository(artifactFromNodeType.getArtifactRepository());
        currentArtifact.setRepositoryName(artifactFromNodeType.getRepositoryName());
        currentArtifact.setRepositoryURL(artifactFromNodeType.getRepositoryURL());
        currentArtifact.setRepositoryCredential(artifactFromNodeType.getRepositoryCredential());
    }
}