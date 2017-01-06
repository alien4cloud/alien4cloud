package org.alien4cloud.tosca.topology;

import java.util.*;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.editor.EditionContext;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.*;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.topology.AbstractTopologyDTO;
import alien4cloud.topology.DependencyConflictDTO;
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

        topologyDTO.setDependencyConflicts(getDependencyConflictDTOs(context));

        // FIXME add validation information
        return topologyDTO;
    }

    /**
     * Compute a list of transitive dependency conflicts from the Context.
     * @param context the EditionContext of the Topology being built.
     * @return a list of dependency conflicts.
     */
    private List<DependencyConflictDTO> getDependencyConflictDTOs(EditionContext context) {
        // Generate a map with all transitive dependency conflict for each dependency in the context.
        final Set<CSARDependency> dependencies = context.getToscaContext().getDependencies();
        Map<CSARDependency, Set<CSARDependency>> dependencyConflictMap = new HashMap<>();
        dependencies.forEach(source -> {
            final Set<CSARDependency> transitives =
                    Optional.ofNullable(ToscaContext.get().getArchive(source.getName(), source.getVersion()).getDependencies())
                            .orElse(Collections.emptySet())
                            .stream().filter(o -> !dependencies.contains(o)).collect(Collectors.toSet());
            if (!transitives.isEmpty()) {
                dependencyConflictMap.put(source, transitives);
            }
        });

        final ArrayList<DependencyConflictDTO> dependencyConflicts = new ArrayList<>();
        dependencyConflictMap.forEach((source, conflicts) ->
            conflicts.forEach(conflict -> {
                String actualVersion = dependencies.stream().filter(d -> d.getName().equals(conflict.getName())).findFirst().map(CSARDependency::getVersion).orElse("");
                dependencyConflicts.add(new DependencyConflictDTO(source.getName(), conflict.getName() + ":" + conflict.getVersion(), actualVersion));
            })
        );
        return dependencyConflicts;
    }

    /**
     * Build a topology dto from a topology.
     *
     * @param topology The topology from which to build the DTO object.
     * @param <T> The type of topology (can be a topology or a deployment topology)
     * @return An instance of TopologyDTO (FIXME Should return an Abstract Topology DTO and renamed as not abstract)
     */
    @ToscaContextual
    public <T extends Topology> TopologyDTO buildTopologyDTO(T topology) {
        TopologyDTO topologyDTO = new TopologyDTO();
        if (topology != null) {
            buildAbstractTopologyDTO(topology, topologyDTO);
            // This contains the value ouf output properties. This has nothing to do with capability somehow..
            topologyDTO.setOutputCapabilityProperties(topology.getOutputCapabilityProperties());
        }
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
            if (nodeType != null) {
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

    private <T extends AbstractInheritableToscaType> Map<String, DataType> fillDataTypes(Map<String, DataType> indexedDataTypes, Map<String, T> elements) {
        for (AbstractInheritableToscaType indexedNodeType : elements.values()) {
            if (indexedNodeType != null && indexedNodeType.getProperties() != null) {
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