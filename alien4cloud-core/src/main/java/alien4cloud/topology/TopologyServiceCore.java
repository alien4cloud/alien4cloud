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
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
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
            NodeType nodeType = getFromContextIfDefined(NodeType.class, topology.getSubstitutionMapping().getSubstitutionType(), topology.getDependencies(),
                    failOnTypeNotFound);
            nodeTypeMap.put(topology.getSubstitutionMapping().getSubstitutionType(), nodeType);
        }

        return nodeTypeMap;
    }

    private Map<String, NodeType> getIndexedNodeTypesFromDependencies(Map<String, NodeTemplate> nodeTemplates, Set<CSARDependency> dependencies,
            boolean abstractOnly, boolean useTemplateNameAsKey, boolean failOnTypeNotFound) {
        Map<String, NodeType> nodeTypes = Maps.newHashMap();
        if (nodeTemplates == null) {
            return nodeTypes;
        }
        for (Map.Entry<String, NodeTemplate> template : nodeTemplates.entrySet()) {
            if (!nodeTypes.containsKey(template.getValue().getType())) {
                NodeType nodeType = getFromContextIfDefined(NodeType.class, template.getValue().getType(), dependencies, failOnTypeNotFound);
                if (!abstractOnly || nodeType.isAbstract()) {
                    String key = useTemplateNameAsKey ? template.getKey() : template.getValue().getType();
                    nodeTypes.put(key, nodeType);
                }
            }
        }
        return nodeTypes;
    }

    public Map<String, PolicyType> getPolicyTypesFromTopology(Topology topology, boolean failOnTypeNotFound) {
        Map<String, PolicyType> types = Maps.newHashMap();
        for (PolicyTemplate template : safe(topology.getPolicies()).values()) {
            types.put(template.getType(), getFromContextIfDefined(PolicyType.class, template.getType(), topology.getDependencies(), failOnTypeNotFound));
        }
        return types;
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
                        RelationshipType relationshipType = getFromContextIfDefined(RelationshipType.class, relationship.getType(), topology.getDependencies(),
                                failOnTypeNotFound);
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
            RelationshipType relationshipType = getFromContextIfDefined(RelationshipType.class, substitutionTarget.getServiceRelationshipType(),
                    topology.getDependencies(), failOnTypeNotFound);
            relationshipTypes.put(substitutionTarget.getServiceRelationshipType(), relationshipType);
        }
    }

    /**
     * Get all capability types used in a topology
     *
     * @param topology the topology to find all relationship types
     * @return The map that contains the capability types.
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
                        CapabilityType capabilityType = getFromContextIfDefined(CapabilityType.class, capability.getType(), topology.getDependencies(), true);
                        capabilityTypes.put(capability.getType(), capabilityType);
                    }
                }
            }
        }
        return capabilityTypes;
    }

    private <T extends AbstractToscaType> T getFromContextIfDefined(Class<T> elementClass, String elementId, Set<CSARDependency> dependencies,
            boolean failOnTypeNotFound) {
        T toscaType = null;
        if (ToscaContext.get() != null) {
            toscaType = ToscaContext.get(elementClass, elementId);
        }
        if (toscaType == null) {
            toscaType = failOnTypeNotFound ? csarRepoSearchService.getRequiredElementInDependencies(elementClass, elementId, dependencies)
                    : csarRepoSearchService.getElementInDependencies(elementClass, elementId, dependencies);
            if (toscaType != null) {
                log.info("NOT FOUND IN TOSCA CONTEXT BUT WAS IN REPO SEARCH SERVICE, context: ", ToscaContext.get());
            }
        }
        return toscaType;
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
