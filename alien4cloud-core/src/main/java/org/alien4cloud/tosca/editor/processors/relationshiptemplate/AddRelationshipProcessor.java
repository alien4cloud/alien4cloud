package org.alien4cloud.tosca.editor.processors.relationshiptemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.exception.CapabilityBoundException;
import org.alien4cloud.tosca.editor.exception.RequirementBoundException;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import org.alien4cloud.tosca.utils.TopologyUtils;
import alien4cloud.topology.validation.TopologyCapabilityBoundsValidationServices;
import alien4cloud.topology.validation.TopologyRequirementBoundsValidationServices;
import alien4cloud.tosca.topology.TemplateBuilder;
import alien4cloud.utils.AlienUtils;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Component
public class AddRelationshipProcessor extends AbstractNodeProcessor<AddRelationshipOperation> {
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Resource
    private TopologyService topologyService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Resource
    private TopologyRequirementBoundsValidationServices topologyRequirementBoundsValidationServices;
    @Resource
    private TopologyCapabilityBoundsValidationServices topologyCapabilityBoundsValidationServices;

    @Override
    protected void processNodeOperation(Csar csar, Topology topology, AddRelationshipOperation operation, NodeTemplate sourceNode) {
        if (operation.getRelationshipName() == null || operation.getRelationshipName().isEmpty()) {
            throw new InvalidNameException("relationshipName", operation.getRelationshipName(), "Not null or empty");
        }

        if (AlienUtils.safe(sourceNode.getRelationships()).containsKey(operation.getRelationshipName())) {
            throw new AlreadyExistException("Relationship " + operation.getRelationshipName() + " already exist on node " + operation.getNodeName());
        }

        if (sourceNode.getRequirements() == null || sourceNode.getRequirements().get(operation.getRequirementName()) == null) {
            throw new NotFoundException(
                    "Unable to find requirement with name <" + operation.getRequirementName() + "> on the source node" + operation.getNodeName());
        }

        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);
        // ensure that the target node exists
        TopologyUtils.getNodeTemplate(topology.getId(), operation.getTarget(), nodeTemplates);

        // We don't use the tosca context as the relationship type may not be in dependencies yet (that's why we use the load type below).
        RelationshipType indexedRelationshipType = toscaTypeSearchService.find(RelationshipType.class, operation.getRelationshipType(),
                operation.getRelationshipVersion());
        if (indexedRelationshipType == null) {
            throw new NotFoundException(RelationshipType.class.getName(), operation.getRelationshipType() + ":" + operation.getRelationshipVersion(),
                    "Unable to find relationship type to create template in topology.");
        }

        boolean upperBoundReachedSource = topologyRequirementBoundsValidationServices.isRequirementUpperBoundReachedForSource(sourceNode,
                operation.getRequirementName(), topology.getDependencies());
        if (upperBoundReachedSource) {
            // throw exception here
            throw new RequirementBoundException(operation.getNodeName(), operation.getRequirementName());
        }

        boolean upperBoundReachedTarget = topologyCapabilityBoundsValidationServices.isCapabilityUpperBoundReachedForTarget(operation.getTarget(),
                nodeTemplates, operation.getTargetedCapabilityName(), topology.getDependencies());
        // return with a rest response error
        if (upperBoundReachedTarget) {
            throw new CapabilityBoundException(operation.getTarget(), operation.getTargetedCapabilityName());
        }

        topologyService.loadType(topology, indexedRelationshipType);

        Map<String, RelationshipTemplate> relationships = sourceNode.getRelationships();
        if (relationships == null) {
            relationships = Maps.newHashMap();
            sourceNode.setRelationships(relationships);
        }

        RelationshipTemplate relationshipTemplate = new RelationshipTemplate();
        relationshipTemplate.setName(operation.getRelationshipName());
        relationshipTemplate.setTarget(operation.getTarget());
        relationshipTemplate.setTargetedCapabilityName(operation.getTargetedCapabilityName());
        relationshipTemplate.setRequirementName(operation.getRequirementName());
        relationshipTemplate.setRequirementType(sourceNode.getRequirements().get(operation.getRequirementName()).getType());
        relationshipTemplate.setType(indexedRelationshipType.getElementId());
        relationshipTemplate.setArtifacts(newLinkedHashMap(indexedRelationshipType.getArtifacts()));
        relationshipTemplate.setAttributes(newLinkedHashMap(indexedRelationshipType.getAttributes()));
        Map<String, AbstractPropertyValue> properties = new LinkedHashMap<String, AbstractPropertyValue>();
        TemplateBuilder.fillProperties(properties, indexedRelationshipType.getProperties(), null);
        relationshipTemplate.setProperties(properties);

        relationships.put(operation.getRelationshipName(), relationshipTemplate);
        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology, csar);
        workflowBuilderService.addRelationship(topologyContext, operation.getNodeName(), operation.getRelationshipName());
        log.debug("Added relationship to the topology [" + topology.getId() + "], node name [" + operation.getNodeName() + "], relationship name ["
                + operation.getRelationshipName() + "]");
    }

    private <T, V> Map<T, V> newLinkedHashMap(Map<T, V> from) {
        if (from == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(from);
    }
}
