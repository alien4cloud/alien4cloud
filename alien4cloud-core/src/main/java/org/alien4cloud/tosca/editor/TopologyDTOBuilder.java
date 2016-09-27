package org.alien4cloud.tosca.editor;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PrimitiveDataType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.topology.AbstractTopologyDTO;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.normative.ToscaType;

/**
 * Service that helps to create a topology dto object out of a topology.
 */
@Service
public class TopologyDTOBuilder {
    /**
     * Build a topology dto (topology and all used types) out of a topology.
     * 
     * @param context The edition context from which to build the dto.
     */
    @ToscaContextual
    public TopologyDTO buildTopologyDTO(EditionContext context) {
        TopologyDTO topologyDTO = new TopologyDTO();
        buildAbstractTopologyDTO(context.getTopology(), topologyDTO);
        topologyDTO.setArchiveContentTree(context.getArchiveContentTree());
        topologyDTO.setLastOperationIndex(context.getLastOperationIndex());
        topologyDTO.setOperations(context.getOperations());
        topologyDTO.setDelegateType(context.getCsar().getDelegateType());
        // FIXME add validation information
        return topologyDTO;
    }

    private <T extends Topology> void buildAbstractTopologyDTO(T topology, AbstractTopologyDTO<T> topologyDTO) {
        topologyDTO.setTopology(topology);
        topologyDTO.setNodeTypes(getNodeTypes(topology));
        topologyDTO.setRelationshipTypes(getRelationshipTypes(topology));
        topologyDTO.setCapabilityTypes(getCapabilityTypes(topologyDTO));
        topologyDTO.setDataTypes(getDataTypes(topologyDTO));
    }

    private <T extends Topology> Map<String, NodeType> getNodeTypes(T topology) {
        Map<String, NodeType> types = Maps.newHashMap();
        fillTypeMap(NodeType.class, types, topology.getNodeTemplates(), false, false);
        return types;
    }

    private <T extends Topology> Map<String, RelationshipType> getRelationshipTypes(T topology) {
        Map<String, RelationshipType> types = Maps.newHashMap();
        if (topology.getNodeTemplates() != null) {
            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                fillTypeMap(RelationshipType.class, types, nodeTemplate.getRelationships(), false, false);
            }
        }
        return types;
    }

    private <T extends Topology> Map<String, CapabilityType> getCapabilityTypes(AbstractTopologyDTO<T> topologyDTO) {
        Map<String, CapabilityType> types = Maps.newHashMap();
        Map<String, NodeType> delayedNodeTypeAddMap = Maps.newHashMap();
        for (NodeType nodeType : topologyDTO.getNodeTypes().values()) {
            for (CapabilityDefinition capabilityDefinition : nodeType.getCapabilities()) {
                types.put(capabilityDefinition.getType(), ToscaContext.get(CapabilityType.class, capabilityDefinition.getType()));
            }
            for (RequirementDefinition requirementDefinition : nodeType.getRequirements()) {
                CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, requirementDefinition.getType());
                if (capabilityType != null) {
                    types.put(requirementDefinition.getType(), capabilityType);
                } else {
                    // requirements are authorized to be a node type rather than a capability type TODO is it still possible in TOSCA ?
                    NodeType indexedNodeType = ToscaContext.get(NodeType.class, requirementDefinition.getType());
                    // add it to the actual node types map
                    delayedNodeTypeAddMap.put(requirementDefinition.getType(), indexedNodeType);
                }
            }
        }
        for (Map.Entry<String, NodeType> delayedNodeType : delayedNodeTypeAddMap.entrySet()) {
            topologyDTO.getNodeTypes().put(delayedNodeType.getKey(), delayedNodeType.getValue());
        }
        return types;
    }

    private <T extends AbstractInheritableToscaType, V extends AbstractTemplate> void fillTypeMap(Class<T> elementClass, Map<String, T> types,
                                                                                                  Map<String, V> templateMap, boolean useTemplateNameAsKey, boolean abstractOnly) {
        if (templateMap == null) {
            return;
        }
        for (Map.Entry<String, V> template : templateMap.entrySet()) {
            if (!types.containsKey(template.getValue().getType())) {
                T type = ToscaContext.get(elementClass, template.getValue().getType());
                if (!abstractOnly || type.isAbstract()) {
                    String key = useTemplateNameAsKey ? template.getKey() : template.getValue().getType();
                    types.put(key, type);
                }
            }
        }
    }

    private Map<String, DataType> getDataTypes(AbstractTopologyDTO topologyDTO) {
        Map<String, DataType> indexedDataTypes = Maps.newHashMap();
        indexedDataTypes = fillDataTypes(indexedDataTypes, topologyDTO.getNodeTypes());
        indexedDataTypes = fillDataTypes(indexedDataTypes, topologyDTO.getRelationshipTypes());
        indexedDataTypes = fillDataTypes(indexedDataTypes, topologyDTO.getCapabilityTypes());
        return indexedDataTypes;
    }

    private <T extends AbstractInheritableToscaType> Map<String, DataType> fillDataTypes(Map<String, DataType> indexedDataTypes,
                                                                                         Map<String, T> elements) {
        for (AbstractInheritableToscaType indexedNodeType : elements.values()) {
            if (indexedNodeType.getProperties() != null) {
                for (PropertyDefinition pd : indexedNodeType.getProperties().values()) {
                    String type = pd.getType();
                    if (ToscaType.isPrimitive(type) || indexedDataTypes.containsKey(type)) {
                        continue;
                    }
                    DataType dataType = ToscaContext.get(DataType.class, type);
                    if (dataType == null) {
                        dataType = ToscaContext.get(PrimitiveDataType.class, type);
                    }
                    indexedDataTypes.put(type, dataType);
                }
            }
        }
        return indexedDataTypes;
    }
}