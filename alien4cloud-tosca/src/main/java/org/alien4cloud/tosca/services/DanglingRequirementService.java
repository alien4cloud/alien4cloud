package org.alien4cloud.tosca.services;

import static alien4cloud.utils.AlienUtils.safe;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.alien4cloud.tosca.utils.NodeTemplateUtils.countRelationshipsForRequirement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.FilterDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.AlienCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.alien4cloud.tosca.normative.constants.NormativeTypesConstant;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.alien4cloud.tosca.utils.NodeTypeUtils;
import org.alien4cloud.tosca.utils.TopologyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.postprocess.ICapabilityMatcherService;
import alien4cloud.tosca.topology.TemplateBuilder;

/**
 * Dangling requirement utility is responsible for generation of node templates with filters from dangling requirements.
 *
 * This can be done on a specific node or on a full topology.
 */
@Component
public class DanglingRequirementService {
    @Resource
    private WorkflowsBuilderService workflowsBuilderService;
    @Resource
    private ICapabilityMatcherService capabilityMatcherService;
    @Resource
    private ICSARRepositorySearchService repositorySearchService;

    @Value("${features.editor_auto_completion}")
    private boolean enabled;

    /**
     * Add dangling requirement nodes for the specified node in the topology.
     *
     * @param topology The topology template
     * @param nodeTemplate The specific node template for which to add dangling requirements.
     */
    public void addDanglingRequirements(Topology topology, WorkflowsBuilderService.TopologyContext topologyContext, NodeTemplate nodeTemplate,
            String requirementSkipAutoCompletion) {
        if (!enabled) {
            return;
        }
        // Get the node type
        NodeType nodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
        for (RequirementDefinition requirementDefinition : nodeType.getRequirements()) {
            if (!requirementDefinition.getId().equals(requirementSkipAutoCompletion)) {
                // let's count current requirement fullfillment count
                int relationshipCount = countRelationshipsForRequirement(nodeTemplate, requirementDefinition);

                if (requirementDefinition.getLowerBound() > relationshipCount) {
                    // we need to add some dangling requirement nodes
                    addDanglingNodes(topology, topologyContext, nodeTemplate, requirementDefinition, requirementDefinition.getLowerBound() - relationshipCount);
                }
            }
        }
    }

    private void addDanglingNodes(Topology topology, WorkflowsBuilderService.TopologyContext topologyContext, NodeTemplate nodeTemplate,
            RequirementDefinition requirementDefinition, int count) {
        // TODO If the TOSCA context does not has the TOSCA normative types then add it automatically
        String danglingTemplateType = requirementDefinition.getNodeType() == null ? NormativeTypesConstant.ROOT_NODE_TYPE : requirementDefinition.getNodeType();
        NodeType danglingNodeType = ToscaContext.get(NodeType.class, danglingTemplateType);

        List<CapabilityDefinition> compatibleCapabilityByType = capabilityMatcherService.getCompatibleCapabilityByType(danglingNodeType,
                requirementDefinition.getType());
        CapabilityDefinition targetCapabilityDefinition = compatibleCapabilityByType.size() == 0 ? null : compatibleCapabilityByType.get(0);

        RelationshipType danglingRelationshipType = fetchValidRelationshipType(requirementDefinition, targetCapabilityDefinition);

        // check if the type is scalable (then count is used as a scalability parameter) or if we should add multiple instances
        CapabilityDefinition scalable = NodeTypeUtils.getCapabilityByType(danglingNodeType, NormativeCapabilityTypes.SCALABLE);
        if (scalable == null) {
            scalable = NodeTypeUtils.getCapabilityByType(danglingNodeType, AlienCapabilityTypes.CLUSTER_CONTROLLER);
        }

        List<NodeTemplate> addedNodes = Lists.newArrayList();

        if (scalable == null) {
            for (int i = 0; i < count; i++) {
                NodeTemplate addedNode = addDanglingNode(topology, topologyContext, nodeTemplate, requirementDefinition, danglingNodeType,
                        danglingRelationshipType, targetCapabilityDefinition);
                addedNodes.add(addedNode);
            }
        } else {
            NodeTemplate danglingTemplate = addDanglingNode(topology, topologyContext, nodeTemplate, requirementDefinition, danglingNodeType,
                    danglingRelationshipType, targetCapabilityDefinition);
            Capability scalableCapability = danglingTemplate.getCapabilities().get(scalable.getId());
            TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, count, scalableCapability);
            TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_MAX_INSTANCES, requirementDefinition.getUpperBound(), scalableCapability);
            addedNodes.add(danglingTemplate);
        }

        // Recursively add dangling nodes.
        for (NodeTemplate addedNode : addedNodes) {
            addDanglingRequirements(topology, topologyContext, addedNode, null);
        }
    }

    public RelationshipType fetchValidRelationshipType(RequirementDefinition requirementDefinition, CapabilityDefinition targetCapabilityDefinition) {
        if (requirementDefinition.getRelationshipType() != null) {
            return ToscaContext.get(RelationshipType.class, requirementDefinition.getRelationshipType());
        }
        if (targetCapabilityDefinition == null) {
            return ToscaContext.get(RelationshipType.class, NormativeRelationshipConstants.ROOT);
        }
        // Let's fetch relationship types that matches the
        RelationshipType relationshipType = repositorySearchService.getElementInDependencies(RelationshipType.class, ToscaContext.get().getDependencies(),
                "validTargets", targetCapabilityDefinition.getType());
        if (relationshipType == null) {
            return ToscaContext.get(RelationshipType.class, NormativeRelationshipConstants.ROOT);
        }
        return relationshipType;
    }

    private NodeTemplate addDanglingNode(Topology topology, WorkflowsBuilderService.TopologyContext topologyContext, NodeTemplate nodeTemplate,
            RequirementDefinition requirementDefinition, NodeType danglingNodeType, RelationshipType danglingRelationshipType,
            CapabilityDefinition targetCapabilityDefinition) {
        NodeTemplate danglingTemplate = TemplateBuilder.buildNodeTemplate(danglingNodeType);

        // Add the filter as defined in the node type.
        danglingTemplate.setNodeFilter(requirementDefinition.getNodeFilter());

        // generate the dangling template name
        String danglingTemplateName = TopologyUtils.getNexAvailableName(nodeTemplate.getName() + "_" + requirementDefinition.getId(), "_",
                topology.getNodeTemplates().keySet());
        danglingTemplate.setName(danglingTemplateName);

        topology.getNodeTemplates().put(danglingTemplateName, danglingTemplate);
        workflowsBuilderService.addNode(topologyContext, danglingTemplateName);

        // Add the dangling requirement relationship
        if (nodeTemplate.getRelationships() == null) {
            nodeTemplate.setRelationships(Maps.newHashMap());
        }

        String danglingRelationshipTemplateName = TopologyUtils.getNexAvailableName(nodeTemplate.getName() + "_" + requirementDefinition.getId(), "_",
                nodeTemplate.getRelationships().keySet());

        RelationshipTemplate relationshipTemplate = new RelationshipTemplate();
        relationshipTemplate.setName(danglingRelationshipTemplateName);
        relationshipTemplate.setTarget(danglingTemplateName);

        String targetCapabilityName = targetCapabilityDefinition == null ? null : targetCapabilityDefinition.getId();
        relationshipTemplate.setTargetedCapabilityName(targetCapabilityName);

        relationshipTemplate.setRequirementName(requirementDefinition.getId());
        relationshipTemplate.setRequirementType(requirementDefinition.getType());
        relationshipTemplate.setType(danglingRelationshipType.getElementId());
        relationshipTemplate.setArtifacts(newLinkedHashMap(safe(danglingRelationshipType.getArtifacts())));
        relationshipTemplate.setAttributes(newLinkedHashMap(safe(danglingRelationshipType.getAttributes())));
        Map<String, AbstractPropertyValue> properties = new LinkedHashMap();
        TemplateBuilder.fillProperties(properties, danglingRelationshipType.getProperties(), null);
        relationshipTemplate.setProperties(properties);

        nodeTemplate.getRelationships().put(danglingRelationshipTemplateName, relationshipTemplate);

        workflowsBuilderService.addRelationship(topologyContext, nodeTemplate.getName(), danglingRelationshipTemplateName);

        // TODO remove this workaround as soon as matchin leverages
        setPropertiesFromFilter(danglingTemplate, danglingNodeType);

        return danglingTemplate;
    }

    // TODO This is a workaround as right now matching does not use filter but properties.
    private void setPropertiesFromFilter(NodeTemplate danglingTemplate, NodeType danglingNodeType) {
        if (danglingTemplate.getNodeFilter() == null) {
            return;
        }

        for (Entry<String, List<PropertyConstraint>> constraintEntry : safe(danglingTemplate.getNodeFilter().getProperties()).entrySet()) {
            if (constraintEntry.getValue().size() == 1 && constraintEntry.getValue().get(0) instanceof EqualConstraint
                    && ToscaTypes.isSimple(danglingNodeType.getProperties().get(constraintEntry.getKey()).getType())) {
                danglingTemplate.getProperties().put(constraintEntry.getKey(),
                        new ScalarPropertyValue(((EqualConstraint) constraintEntry.getValue().get(0)).getEqual()));
            }
        }

        for (Entry<String, FilterDefinition> capabilityFilter : safe(danglingTemplate.getNodeFilter().getCapabilities()).entrySet()) {
            Capability capability = getCapability(danglingTemplate, capabilityFilter.getKey());
            CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capability.getType());
            for (Entry<String, List<PropertyConstraint>> constraintEntry : safe(capabilityFilter.getValue().getProperties()).entrySet()) {
                if (constraintEntry.getValue().size() == 1 && constraintEntry.getValue().get(0) instanceof EqualConstraint
                        && ToscaTypes.isSimple(capabilityType.getProperties().get(constraintEntry.getKey()).getType())) {
                    capability.getProperties().put(constraintEntry.getKey(),
                            new ScalarPropertyValue(((EqualConstraint) constraintEntry.getValue().get(0)).getEqual()));
                }
            }
        }
    }

    private Capability getCapability(NodeTemplate danglingTemplate, String filterCapabilityKey) {
        for (Entry<String, Capability> capabilityEntry : safe(danglingTemplate.getCapabilities()).entrySet()) {
            if (filterCapabilityKey.equals(capabilityEntry.getKey()) || filterCapabilityKey.equals(capabilityEntry.getValue().getType())) {
                return capabilityEntry.getValue();
            }
        }
        return null;
    }
}