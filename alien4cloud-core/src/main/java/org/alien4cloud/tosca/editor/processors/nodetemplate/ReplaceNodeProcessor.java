package org.alien4cloud.tosca.editor.processors.nodetemplate;

import static alien4cloud.utils.AlienUtils.safe;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.operations.nodetemplate.ReplaceNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.services.DanglingRequirementService;
import org.alien4cloud.tosca.utils.NodeTemplateUtils;
import org.alien4cloud.tosca.utils.NodeTypeUtils;
import org.alien4cloud.tosca.utils.TopologyUtils;
import org.alien4cloud.tosca.utils.TopologyUtils.RelationshipEntry;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.topology.TemplateBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Replace the type of a node template by another compatible type (inherited or that fulfills the same used capabilities and requirements).
 */
@Slf4j
@Component
public class ReplaceNodeProcessor implements IEditorOperationProcessor<ReplaceNodeOperation> {
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private DanglingRequirementService danglingRequirementService;

    @Override
    public void process(Csar csar, Topology topology, ReplaceNodeOperation operation) {
        // Retrieve existing node template
        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);
        NodeTemplate oldNodeTemplate = TopologyUtils.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);

        String[] splittedId = operation.getNewTypeId().split(":");

        // Unload and remove old node template
        topologyService.unloadType(topology, oldNodeTemplate.getType());
        // remove the node from the topology and the workflows
        nodeTemplates.remove(oldNodeTemplate.getName());
        workflowBuilderService.removeNode(topology, csar, oldNodeTemplate.getName());

        // Build the new one
        NodeType newType = toscaTypeSearchService.find(NodeType.class, splittedId[0], splittedId[1]);
        // Load the new type to the topology in order to update its dependencies
        newType = topologyService.loadType(topology, newType);
        NodeTemplate newNodeTemplate = TemplateBuilder.buildNodeTemplate(newType, oldNodeTemplate, false);
        newNodeTemplate.setName(operation.getNodeName());
        newNodeTemplate.setTags(oldNodeTemplate.getTags());
        newNodeTemplate.setName(oldNodeTemplate.getName());
        newNodeTemplate.setRelationships(oldNodeTemplate.getRelationships());
        // Put the new one in the topology
        nodeTemplates.put(oldNodeTemplate.getName(), newNodeTemplate);
        // When replacing a node with another some relationships target capabilities or requirements may be impacted and moved to another capability/requirement
        // name.
        updateRelationshipsCapabilitiesRelationships(topology, newNodeTemplate);

        // FIXME we should remove outputs/inputs, others here ?
        if (topology.getSubstitutionMapping() != null) {
            removeNodeTemplateSubstitutionTargetMapEntry(oldNodeTemplate.getName(), topology.getSubstitutionMapping().getCapabilities());
            removeNodeTemplateSubstitutionTargetMapEntry(oldNodeTemplate.getName(), topology.getSubstitutionMapping().getRequirements());
        }

        log.debug("Replacing the node template[ {} ] with [ {} ] bound to the node type [ {} ] on the topology [ {} ] .", oldNodeTemplate.getName(),
                oldNodeTemplate.getName(), operation.getNewTypeId(), topology.getId());

        // add the new node to the workflow
        TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology, csar);
        workflowBuilderService.addNode(topologyContext, oldNodeTemplate.getName());
        // add the relationships from the new node to the workflow
        safe(newNodeTemplate.getRelationships()).forEach(
                (relationshipId, relationshipTemplate) -> workflowBuilderService.addRelationship(topologyContext, newNodeTemplate.getName(), relationshipId));
        // add the relationships to the new node to the workflow
        TopologyUtils.getTargetRelationships(oldNodeTemplate.getName(), nodeTemplates).forEach(relationshipEntry -> workflowBuilderService
                .addRelationship(topologyContext, relationshipEntry.getSource().getName(), relationshipEntry.getRelationshipId()));
        if(!operation.isSkipAutoCompletion()) {
            danglingRequirementService.addDanglingRequirements(topology, topologyContext, newNodeTemplate, null);
        }
    }

    private void removeNodeTemplateSubstitutionTargetMapEntry(String nodeTemplateName, Map<String, SubstitutionTarget> substitutionTargets) {
        if (substitutionTargets == null) {
            return;
        }
        substitutionTargets.entrySet().removeIf(e -> e.getValue().getNodeTemplateName().equals(nodeTemplateName));
    }

    private void updateRelationshipsCapabilitiesRelationships(Topology topology, NodeTemplate nodeTemplate) {
        List<RelationshipEntry> targetRelationships = TopologyUtils.getTargetRelationships(nodeTemplate.getName(), topology.getNodeTemplates());
        for (RelationshipEntry targetRelationshipEntry : targetRelationships) {
            RelationshipTemplate targetRelationship = targetRelationshipEntry.getRelationship();
            Capability capability = safe(nodeTemplate.getCapabilities()).get(targetRelationship.getTargetedCapabilityName());
            if (capability == null || isCapabilityNotOfType(capability, targetRelationship.getRequirementType())) {
                Entry<String, Capability> capabilityEntry = NodeTemplateUtils.getCapabilitEntryyByType(nodeTemplate, targetRelationship.getRequirementType());
                targetRelationship.setTargetedCapabilityName(capabilityEntry.getKey());
                // check that the relationship type is still valid with the new capability
                RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, targetRelationship.getType());
                if (!isValidRelationship(relationshipType, capabilityEntry.getValue())) {
                    NodeType sourceNodeType = ToscaContext.get(NodeType.class, targetRelationshipEntry.getSource().getType());
                    RequirementDefinition requirementDefinition = NodeTypeUtils.getRequirementById(sourceNodeType,
                            targetRelationshipEntry.getRelationship().getRequirementName());
                    NodeType targetNodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
                    CapabilityDefinition capabilityDefinition = NodeTypeUtils.getCapabilityById(targetNodeType, capabilityEntry.getKey());
                    RelationshipType validRelationshipType = danglingRequirementService.fetchValidRelationshipType(requirementDefinition, capabilityDefinition);

                    targetRelationship.setType(validRelationshipType.getElementId());
                    targetRelationship.setType(validRelationshipType.getElementId());
                    targetRelationship.setArtifacts(newLinkedHashMap(safe(validRelationshipType.getArtifacts())));
                    targetRelationship.setAttributes(newLinkedHashMap(safe(validRelationshipType.getAttributes())));
                    Map<String, AbstractPropertyValue> properties = new LinkedHashMap();
                    TemplateBuilder.fillProperties(properties, validRelationshipType.getProperties(), null);
                    targetRelationship.setProperties(properties);
                }
            }
        }
    }

    private boolean isValidRelationship(RelationshipType relationshipType, Capability targetCapability) {
        if (relationshipType.getValidTargets() == null) {
            return false;
        }

        for (String target : relationshipType.getValidTargets()) {
            if (targetCapability.getType().equals(target)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCapabilityNotOfType(Capability capability, String expectedCapabilityType) {
        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capability.getType());
        return !ToscaTypeUtils.isOfType(capabilityType, expectedCapabilityType);
    }
}
