package org.alien4cloud.tosca.editor;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.model.components.*;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
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
        buildAbstractTopologyDTO(context.getCurrentTopology(), topologyDTO);
        topologyDTO.setArchiveContentTree(context.getArchiveContentTree());
        topologyDTO.setLastOperationIndex(context.getLastOperationIndex());
        topologyDTO.setOperations(context.getOperations());
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

    private <T extends Topology> Map<String, IndexedNodeType> getNodeTypes(T topology) {
        Map<String, IndexedNodeType> types = Maps.newHashMap();
        fillTypeMap(IndexedNodeType.class, types, topology.getNodeTemplates(), false, false);
        return types;
    }

    private <T extends Topology> Map<String, IndexedRelationshipType> getRelationshipTypes(T topology) {
        Map<String, IndexedRelationshipType> types = Maps.newHashMap();
        if (topology.getNodeTemplates() != null) {
            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                fillTypeMap(IndexedRelationshipType.class, types, nodeTemplate.getRelationships(), false, false);
            }
        }
        return types;
    }

    private <T extends Topology> Map<String, IndexedCapabilityType> getCapabilityTypes(AbstractTopologyDTO<T> topologyDTO) {
        Map<String, IndexedCapabilityType> types = Maps.newHashMap();
        for (IndexedNodeType nodeType : topologyDTO.getNodeTypes().values()) {
            for (CapabilityDefinition capabilityDefinition : nodeType.getCapabilities()) {
                types.put(capabilityDefinition.getType(), ToscaContext.get(IndexedCapabilityType.class, capabilityDefinition.getType()));
            }
            for (RequirementDefinition requirementDefinition : nodeType.getRequirements()) {
                IndexedCapabilityType capabilityType = ToscaContext.get(IndexedCapabilityType.class, requirementDefinition.getType());
                if (capabilityType != null) {
                    types.put(requirementDefinition.getType(), capabilityType);
                } else {
                    // requirements are authorized to be a node type rather than a capability type TODO is it still possible in TOSCA ?
                    IndexedNodeType indexedNodeType = ToscaContext.get(IndexedNodeType.class, requirementDefinition.getType());
                    // add it to the actual node types map
                    topologyDTO.getNodeTypes().put(requirementDefinition.getType(), indexedNodeType);
                }
            }
        }
        return types;
    }

    private <T extends IndexedInheritableToscaElement, V extends AbstractTemplate> void fillTypeMap(Class<T> elementClass, Map<String, T> types,
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

    private Map<String, IndexedDataType> getDataTypes(AbstractTopologyDTO topologyDTO) {
        Map<String, IndexedDataType> indexedDataTypes = Maps.newHashMap();
        indexedDataTypes = fillDataTypes(indexedDataTypes, topologyDTO.getNodeTypes());
        indexedDataTypes = fillDataTypes(indexedDataTypes, topologyDTO.getRelationshipTypes());
        indexedDataTypes = fillDataTypes(indexedDataTypes, topologyDTO.getCapabilityTypes());
        return indexedDataTypes;
    }

    private <T extends IndexedInheritableToscaElement> Map<String, IndexedDataType> fillDataTypes(Map<String, IndexedDataType> indexedDataTypes,
            Map<String, T> elements) {
        for (IndexedInheritableToscaElement indexedNodeType : elements.values()) {
            if (indexedNodeType.getProperties() != null) {
                for (PropertyDefinition pd : indexedNodeType.getProperties().values()) {
                    String type = pd.getType();
                    if (ToscaType.isPrimitive(type) || indexedDataTypes.containsKey(type)) {
                        continue;
                    }
                    IndexedDataType dataType = ToscaContext.get(IndexedDataType.class, type);
                    if (dataType == null) {
                        dataType = ToscaContext.get(PrimitiveIndexedDataType.class, type);
                    }
                    indexedDataTypes.put(type, dataType);
                }
            }
        }
        return indexedDataTypes;
    }
}