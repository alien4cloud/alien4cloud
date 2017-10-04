package org.alien4cloud.tosca.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.editor.EditionContext;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.utils.DataTypesFetcher;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.topology.AbstractTopologyDTO;
import alien4cloud.topology.DependencyConflictDTO;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;

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
        initTopologyDTO(context.getTopology(), topologyDTO);
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
     * 
     * @param context the EditionContext of the Topology being built.
     * @return a list of dependency conflicts.
     */
    private List<DependencyConflictDTO> getDependencyConflictDTOs(EditionContext context) {
        // Generate a map with all transitive dependency conflict for each dependency in the context.
        final Set<CSARDependency> dependencies = context.getToscaContext().getDependencies();
        Map<CSARDependency, Set<CSARDependency>> dependencyConflictMap = new HashMap<>();
        dependencies.forEach(source -> {
            final Set<CSARDependency> transitives = Optional.ofNullable(ToscaContext.get().getArchive(source.getName(), source.getVersion()).getDependencies())
                    .orElse(Collections.emptySet()).stream().filter(o -> !dependencies.contains(o)).collect(Collectors.toSet());
            if (!transitives.isEmpty()) {
                dependencyConflictMap.put(source, transitives);
            }
        });

        final ArrayList<DependencyConflictDTO> dependencyConflicts = new ArrayList<>();
        dependencyConflictMap.forEach((source, conflicts) -> conflicts.forEach(conflict -> {
            String actualVersion = dependencies.stream().filter(d -> d.getName().equals(conflict.getName())).findFirst().map(CSARDependency::getVersion)
                    .orElse("");
            dependencyConflicts.add(new DependencyConflictDTO(source.getName(), conflict.getName() + ":" + conflict.getVersion(), actualVersion));
        }));
        return dependencyConflicts;
    }

    /**
     * Initialize an abstract topology DTO by filling in the node types, relationship types, capability types etc. from the context.
     * 
     * @param topology The topology to wrap into a DTO object.
     * @param topologyDTO The topology DTO object that contains the topology and all referenced tosca types.
     * @param <T> The topology type.
     * @param <V> The abstract dto actual type.
     * @return The instance of abstract topology DTO given as a parameter initialized with all the types used in the given topology.
     */
    @ToscaContextual
    public <T extends Topology, V extends AbstractTopologyDTO<T>> V initTopologyDTO(T topology, V topologyDTO) {
        if (topology == null) {
            return topologyDTO;
        }
        topologyDTO.setTopology(topology);
        topologyDTO.setNodeTypes(getNodeTypes(topology));
        topologyDTO.setRelationshipTypes(getRelationshipTypes(topology));
        topologyDTO.setCapabilityTypes(getCapabilityTypes(topologyDTO));
        topologyDTO.setDataTypes(getDataTypes(topologyDTO));
        topologyDTO.setPolicyTypes(getPolicyTypes(topology));
        return topologyDTO;
    }

    private <T extends Topology> Map<String, NodeType> getNodeTypes(T topology) {
        Map<String, NodeType> types = Maps.newHashMap();
        fillTypeMap(NodeType.class, types, topology.getNodeTemplates(), false, false);
        return types;
    }

    private <T extends Topology> Map<String, PolicyType> getPolicyTypes(T topology) {
        Map<String, PolicyType> types = Maps.newHashMap();
        fillTypeMap(PolicyType.class, types, topology.getPolicies(), false, false);
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
        DataTypesFetcher.DataTypeFinder dataTypeFinder = (type, id) -> ToscaContext.get(type, id);
        if (MapUtils.isNotEmpty(topologyDTO.getNodeTypes())) {
            indexedDataTypes.putAll(DataTypesFetcher.getDataTypesDependencies(topologyDTO.getNodeTypes().values(), dataTypeFinder));
        }
        if (MapUtils.isNotEmpty(topologyDTO.getRelationshipTypes())) {
            indexedDataTypes.putAll(DataTypesFetcher.getDataTypesDependencies(topologyDTO.getRelationshipTypes().values(), dataTypeFinder));
        }
        if (MapUtils.isNotEmpty(topologyDTO.getCapabilityTypes())) {
            indexedDataTypes.putAll(DataTypesFetcher.getDataTypesDependencies(topologyDTO.getCapabilityTypes().values(), dataTypeFinder));
        }
        return indexedDataTypes;
    }
}