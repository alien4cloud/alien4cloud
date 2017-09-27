package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.IFileRepository;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Process a {@link DeleteNodeOperation}
 */
@Slf4j
@Component
public class DeleteNodeProcessor extends AbstractNodeProcessor<DeleteNodeOperation> {
    @Resource
    private TopologyService topologyService;
    @Resource
    private IFileRepository artifactRepository;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    protected void processNodeOperation(DeleteNodeOperation operation, NodeTemplate template) {
        Topology topology = EditionContextManager.getTopology();
        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);

        // FIXME cleanup files on the github repository / This way we can commit or revert if not saved.
        // FIXME we SHOULD we delegate all this processing to the save operation as we don't support undo on disk.
        // Clean up internal repository
        Map<String, DeploymentArtifact> artifacts = template.getArtifacts();
        if (artifacts != null) {
            for (Map.Entry<String, DeploymentArtifact> artifactEntry : artifacts.entrySet()) {
                DeploymentArtifact artifact = artifactEntry.getValue();
                if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
                    this.artifactRepository.deleteFile(artifact.getArtifactRef());
                }
            }
        }

        List<String> typesTobeUnloaded = Lists.newArrayList();
        // Clean up dependencies of the topology
        typesTobeUnloaded.add(template.getType());
        if (template.getRelationships() != null) {
            for (RelationshipTemplate relationshipTemplate : template.getRelationships().values()) {
                typesTobeUnloaded.add(relationshipTemplate.getType());
            }
        }
        topologyService.unloadType(topology, typesTobeUnloaded.toArray(new String[typesTobeUnloaded.size()]));

        removeRelationShipReferences(operation.getNodeName(), topology);
        nodeTemplates.remove(operation.getNodeName());
        removeOutputs(operation.getNodeName(), topology);
        if (topology.getSubstitutionMapping() != null) {
            removeNodeTemplateSubstitutionTargetMapEntry(operation.getNodeName(), topology.getSubstitutionMapping().getCapabilities());
            removeNodeTemplateSubstitutionTargetMapEntry(operation.getNodeName(), topology.getSubstitutionMapping().getRequirements());
        }

        // group members removal
        TopologyUtils.updateGroupMembers(topology, template, operation.getNodeName(), null);
        // update the workflows
        workflowBuilderService.removeNode(topology, EditionContextManager.getCsar(), operation.getNodeName());
        log.debug("Removed node template <{}> from the topology <{}> .", operation.getNodeName(), topology.getId());
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
     * Remove all relationships in nodes where the target was the removed node.
     *
     * @param removedNode The name of the removed node
     * @param topology The topology in which to remove the references.
     */
    private void removeRelationShipReferences(String removedNode, Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        List<String> keysToRemove = Lists.newArrayList();
        for (String key : nodeTemplates.keySet()) {
            NodeTemplate nodeTemp = nodeTemplates.get(key);
            if (nodeTemp.getRelationships() == null) {
                continue;
            }
            keysToRemove.clear();
            for (String key2 : nodeTemp.getRelationships().keySet()) {
                RelationshipTemplate relTemp = nodeTemp.getRelationships().get(key2);
                if (relTemp == null) {
                    continue;
                }
                if (relTemp.getTarget() != null && relTemp.getTarget().equals(removedNode)) {
                    keysToRemove.add(key2);
                }
            }
            for (String relName : keysToRemove) {
                nodeTemplates.get(key).getRelationships().remove(relName);
            }
        }
    }
}