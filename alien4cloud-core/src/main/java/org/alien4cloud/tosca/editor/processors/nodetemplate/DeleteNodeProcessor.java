package org.alien4cloud.tosca.editor.processors.nodetemplate;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorCommitableProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.IFileRepository;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import org.alien4cloud.tosca.utils.TopologyUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Process a {@link DeleteNodeOperation}
 */
@Slf4j
@Component
public class DeleteNodeProcessor extends AbstractNodeProcessor<DeleteNodeOperation> implements IEditorCommitableProcessor<DeleteNodeOperation> {
    @Inject
    private TopologyService topologyService;
    @Inject
    private IFileRepository artifactRepository;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    protected void processNodeOperation(Csar csar, Topology topology, DeleteNodeOperation operation, NodeTemplate template) {
        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);

        // Prepare to cleanup files (store artifacts reference in the operation and process deletion on before commit operation).
        Map<String, DeploymentArtifact> artifacts = template.getArtifacts();
        operation.setArtifacts(artifacts);

        List<String> typesTobeUnloaded = Lists.newArrayList();
        // Clean up dependencies of the topology
        removeRelationShipReferences(operation.getNodeName(), csar, topology, typesTobeUnloaded);
        topologyService.unloadType(topology, typesTobeUnloaded.toArray(new String[typesTobeUnloaded.size()]));

        // Cleanup from policies
        removeNodeFromPolicies(operation.getNodeName(), topology);

        nodeTemplates.remove(operation.getNodeName());
        removeOutputs(operation.getNodeName(), topology);
        if (topology.getSubstitutionMapping() != null) {
            removeNodeTemplateSubstitutionTargetMapEntry(operation.getNodeName(), topology.getSubstitutionMapping().getCapabilities());
            removeNodeTemplateSubstitutionTargetMapEntry(operation.getNodeName(), topology.getSubstitutionMapping().getRequirements());
        }

        // group members removal
        TopologyUtils.updateGroupMembers(topology, template, operation.getNodeName(), null);
        // update the workflows
        workflowBuilderService.removeNode(topology, csar, operation.getNodeName());
        log.debug("Removed node template [ {} ] from the topology [ {} ] .", operation.getNodeName(), topology.getId());
    }

    /**
     * Remove a nodeTemplate outputs in a topology
     */
    private void removeOutputs(String nodeTemplateName, Topology topology) {
        if (topology.getOutputProperties() != null) {
            topology.getOutputProperties().remove(nodeTemplateName);
        }
        if (topology.getOutputAttributes() != null) {
            topology.getOutputAttributes().remove(nodeTemplateName);
        }
        if (topology.getOutputCapabilityProperties() != null) {
            topology.getOutputCapabilityProperties().remove(nodeTemplateName);
        }
    }

    private void removeNodeTemplateSubstitutionTargetMapEntry(String nodeTemplateName, Map<String, SubstitutionTarget> substitutionTargets) {
        if (substitutionTargets == null) {
            return;
        }
        Iterator<Map.Entry<String, SubstitutionTarget>> capabilities = substitutionTargets.entrySet().iterator();
        while (capabilities.hasNext()) {
            Map.Entry<String, SubstitutionTarget> e = capabilities.next();
            if (e.getValue().getNodeTemplateName().equals(nodeTemplateName)) {
                capabilities.remove();
            }
        }
    }

    /**
     * Removes all relationships connected to the removed node (either as source or target)
     *
     * @param removedNode The name of the removed node
     * @param topology The topology in which to remove the references.
     * @param typesTobeUnloaded List of types to remove from the topology (when relationships are removed)
     */
    private void removeRelationShipReferences(String removedNode, Csar csar, Topology topology, List<String> typesTobeUnloaded) {
        for (NodeTemplate nodeTemplate : safe(topology.getNodeTemplates()).values()) {
            for (Iterator<RelationshipTemplate> i = safe(nodeTemplate.getRelationships()).values().iterator(); i.hasNext() ;) {
                RelationshipTemplate relationshipTemplate = i.next();

                if (removedNode.equals(relationshipTemplate.getTarget())) {
                    typesTobeUnloaded.add(relationshipTemplate.getType());

                    workflowBuilderService.removeRelationship(topology, csar, nodeTemplate.getName(),
                            relationshipTemplate.getName(), relationshipTemplate);

                    i.remove();
                }
            }
        }
    }

    private void removeNodeFromPolicies(String removedNode, Topology topology) {
        for (PolicyTemplate policyTemplate : safe(topology.getPolicies()).values()) {
            policyTemplate.getTargets().remove(removedNode);
        }
    }

    @Override
    public void beforeCommit(DeleteNodeOperation operation) {
        for (Map.Entry<String, DeploymentArtifact> artifactEntry : safe(operation.getArtifacts()).entrySet()) {
            DeploymentArtifact artifact = artifactEntry.getValue();
            if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
                this.artifactRepository.deleteFile(artifact.getArtifactRef());
            }
        }
    }
}