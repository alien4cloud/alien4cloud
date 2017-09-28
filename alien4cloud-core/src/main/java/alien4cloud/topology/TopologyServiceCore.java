package alien4cloud.topology;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.topology.TemplateBuilder;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TopologyServiceCore {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    public Topology getTopology(String topologyId) {
        return alienDAO.findById(Topology.class, topologyId);
    }

    /**
     * Retrieve a topology given its Id
     *
     * @param topologyId id of the topology
     * @return the found topology, throws NotFoundException if not found
     */
    public Topology getOrFail(String topologyId) {
        Topology topology = getTopology(topologyId);
        if (topology == null) {
            throw new NotFoundException("Topology [" + topologyId + "] cannot be found");
        }
        return topology;
    }

    /**
     * Get the indexed node types used in a topology.
     *
     * @param topology The topology for which to get indexed node types.
     * @param abstractOnly If true, only abstract types will be retrieved.
     * @param useTemplateNameAsKey If true the name of the node template will be used as key for the type in the returned map, if not the type will be used as
     *            key.
     * @param failOnTypeNotFound
     * @return A map of indexed node types.
     */
    public Map<String, NodeType> getIndexedNodeTypesFromTopology(Topology topology, boolean abstractOnly, boolean useTemplateNameAsKey,
            boolean failOnTypeNotFound) {
        Map<String, NodeType> nodeTypeMap = getIndexedNodeTypesFromDependencies(topology.getNodeTemplates(), topology.getDependencies(), abstractOnly,
                useTemplateNameAsKey, failOnTypeNotFound);

        if (!useTemplateNameAsKey && topology.getSubstitutionMapping() != null && topology.getSubstitutionMapping().getSubstitutionType() != null) {
            NodeType nodeType = failOnTypeNotFound
                    ? csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, topology.getSubstitutionMapping().getSubstitutionType(),
                            topology.getDependencies())
                    : csarRepoSearchService.getElementInDependencies(NodeType.class, topology.getSubstitutionMapping().getSubstitutionType(),
                            topology.getDependencies());
            nodeTypeMap.put(topology.getSubstitutionMapping().getSubstitutionType(), nodeType);
        }

        return nodeTypeMap;
    }

    public Map<String, NodeType> getIndexedNodeTypesFromDependencies(Map<String, NodeTemplate> nodeTemplates, Set<CSARDependency> dependencies,
            boolean abstractOnly, boolean useTemplateNameAsKey, boolean failOnTypeNotFound) {

        Map<String, NodeType> nodeTypes = Maps.newHashMap();
        if (nodeTemplates == null) {
            return nodeTypes;
        }
        for (Map.Entry<String, NodeTemplate> template : nodeTemplates.entrySet()) {
            if (!nodeTypes.containsKey(template.getValue().getType())) {
                NodeType nodeType = failOnTypeNotFound
                        ? csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, template.getValue().getType(), dependencies)
                        : csarRepoSearchService.getElementInDependencies(NodeType.class, template.getValue().getType(), dependencies);
                if (!abstractOnly || nodeType.isAbstract()) {
                    String key = useTemplateNameAsKey ? template.getKey() : template.getValue().getType();
                    nodeTypes.put(key, nodeType);
                }
            }
        }
        return nodeTypes;
    }

    /**
     * Get IndexedRelationshipType in a topology
     *
     * @param topology the topology to find all relationship types
     * @param failOnTypeNotFound
     * @return the map containing rel
     */
    public Map<String, RelationshipType> getIndexedRelationshipTypesFromTopology(Topology topology, boolean failOnTypeNotFound) {
        Map<String, RelationshipType> relationshipTypes = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return relationshipTypes;
        }
        for (Map.Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
            NodeTemplate template = templateEntry.getValue();
            if (template.getRelationships() != null) {
                for (Map.Entry<String, RelationshipTemplate> relationshipEntry : template.getRelationships().entrySet()) {
                    RelationshipTemplate relationship = relationshipEntry.getValue();
                    if (!relationshipTypes.containsKey(relationship.getType())) {
                        RelationshipType relationshipType = failOnTypeNotFound
                                ? csarRepoSearchService.getRequiredElementInDependencies(RelationshipType.class, relationship.getType(),
                                        topology.getDependencies())
                                : csarRepoSearchService.getElementInDependencies(RelationshipType.class, relationship.getType(), topology.getDependencies());
                        relationshipTypes.put(relationship.getType(), relationshipType);
                    }
                }
            }
        }

        if (topology.getSubstitutionMapping() != null && topology.getSubstitutionMapping().getSubstitutionType() != null) {
            for (SubstitutionTarget substitutionTarget : safe(topology.getSubstitutionMapping().getCapabilities()).values()) {
                addRelationshipTypeFromSubstitutionTarget(topology, relationshipTypes, substitutionTarget, failOnTypeNotFound);
            }
            for (SubstitutionTarget substitutionTarget : safe(topology.getSubstitutionMapping().getRequirements()).values()) {
                addRelationshipTypeFromSubstitutionTarget(topology, relationshipTypes, substitutionTarget, failOnTypeNotFound);
            }
        }

        return relationshipTypes;
    }

    private void addRelationshipTypeFromSubstitutionTarget(Topology topology, Map<String, RelationshipType> relationshipTypes,
            SubstitutionTarget substitutionTarget, boolean failOnTypeNotFound) {
        if (substitutionTarget.getServiceRelationshipType() != null) {
            RelationshipType relationshipType = failOnTypeNotFound
                    ? csarRepoSearchService.getRequiredElementInDependencies(RelationshipType.class, substitutionTarget.getServiceRelationshipType(),
                            topology.getDependencies())
                    : csarRepoSearchService.getElementInDependencies(RelationshipType.class, substitutionTarget.getServiceRelationshipType(),
                            topology.getDependencies());
            relationshipTypes.put(substitutionTarget.getServiceRelationshipType(), relationshipType);
        }
    }

    /**
     * Get IndexedRelationshipType in a topology
     *
     * @param topology the topology to find all relationship types
     * @return the map containing rel
     */
    public Map<String, CapabilityType> getIndexedCapabilityTypesFromTopology(Topology topology) {
        Map<String, CapabilityType> capabilityTypes = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return capabilityTypes;
        }
        for (Map.Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
            NodeTemplate template = templateEntry.getValue();
            if (template.getCapabilities() != null) {
                for (Map.Entry<String, Capability> capabilityEntry : template.getCapabilities().entrySet()) {
                    Capability capability = capabilityEntry.getValue();
                    if (!capabilityTypes.containsKey(capability.getType())) {
                        CapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(CapabilityType.class, capability.getType(),
                                topology.getDependencies());
                        capabilityTypes.put(capability.getType(), capabilityType);
                    }
                }
            }
        }
        return capabilityTypes;
    }

    /**
     *
     * Get all the relationships in which a given node template is a target
     *
     * @param nodeTemplateName the name of the node template which is target for relationship
     * @param nodeTemplates all topology's node templates
     * @return all relationships which have nodeTemplateName as target
     */
    public List<RelationshipTemplate> getTargetRelatedRelatonshipsTemplate(String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        List<RelationshipTemplate> toReturn = Lists.newArrayList();
        for (String key : nodeTemplates.keySet()) {
            NodeTemplate nodeTemp = nodeTemplates.get(key);
            if (nodeTemp.getRelationships() == null) {
                continue;
            }
            for (String key2 : nodeTemp.getRelationships().keySet()) {
                RelationshipTemplate relTemp = nodeTemp.getRelationships().get(key2);
                if (relTemp == null) {
                    continue;
                }
                if (relTemp.getTarget() != null && relTemp.getTarget().equals(nodeTemplateName)) {
                    toReturn.add(relTemp);
                }
            }
        }

        return toReturn;
    }

    /**
     * Assign an id to the topology, save it and return the generated id.
     *
     * @param topology
     * @return
     */
    public String saveTopology(Topology topology) {
        String topologyId = UUID.randomUUID().toString();
        topology.setId(topologyId);
        save(topology);
        return topologyId;
    }

    public void save(Topology topology) {
        this.alienDAO.save(topology);
    }

}
