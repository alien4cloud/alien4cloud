package org.alien4cloud.tosca.editor.processors;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.operations.AddRelationshipOperation;
import org.alien4cloud.tosca.editor.exception.CapabilityBoundException;
import org.alien4cloud.tosca.editor.exception.RequirementBoundException;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.validation.TopologyCapabilityBoundsValidationServices;
import alien4cloud.topology.validation.TopologyRequirementBoundsValidationServices;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Component
public class AddRelationshipTemplateProcessor extends AbstractNodeProcessor<AddRelationshipOperation> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TopologyService topologyService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Resource
    private TopologyRequirementBoundsValidationServices topologyRequirementBoundsValidationServices;
    @Resource
    private TopologyCapabilityBoundsValidationServices topologyCapabilityBoundsValidationServices;

    @Override
    @SneakyThrows
    protected void processNodeOperation(AddRelationshipOperation operation, NodeTemplate sourceNode) {
        Topology topology = TopologyEditionContextManager.getTopology();
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);

        String relationshipId = operation.getRelationshipType() + ":" + operation.getRelationshipVersion();
        IndexedRelationshipType indexedRelationshipType = alienDAO.findById(IndexedRelationshipType.class, relationshipId);
        if (indexedRelationshipType == null) {
            throw new NotFoundException(IndexedRelationshipType.class.getName(), relationshipId,
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

        // FIXME impact ToscaContext
        topologyService.loadType(topology, indexedRelationshipType);

        Map<String, RelationshipTemplate> relationships = sourceNode.getRelationships();
        if (relationships == null) {
            relationships = Maps.newHashMap();
            sourceNode.setRelationships(relationships);
        }

        RelationshipTemplate relationshipTemplate = new RelationshipTemplate();
        relationshipTemplate.setTarget(operation.getTarget());
        relationshipTemplate.setTargetedCapabilityName(operation.getTargetedCapabilityName());
        relationshipTemplate.setRequirementName(operation.getRequirementName());
        relationshipTemplate.setRequirementType(sourceNode.getRequirements().get(operation.getRequirementName()).getType());
        relationshipTemplate.setType(indexedRelationshipType.getElementId());
        relationshipTemplate.setArtifacts(new LinkedHashMap<>(indexedRelationshipType.getArtifacts()));
        relationshipTemplate.setAttributes(new LinkedHashMap<>(indexedRelationshipType.getAttributes()));
        Map<String, AbstractPropertyValue> properties = new LinkedHashMap<String, AbstractPropertyValue>();
        NodeTemplateBuilder.fillProperties(properties, indexedRelationshipType.getProperties(), null);
        relationshipTemplate.setProperties(properties);
        relationshipTemplate.setInterfaces(indexedRelationshipType.getInterfaces());

        relationships.put(operation.getRelationshipName(), relationshipTemplate);
        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);
        workflowBuilderService.addRelationship(topologyContext, operation.getNodeName(), operation.getRelationshipName());
        log.debug("Added relationship to the topology [" + topology.getId() + "], node name [" + operation.getNodeName() + "], relationship name ["
                + operation.getRelationshipName() + "]");
    }
}
