package alien4cloud.topology;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedRelationshipType;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.container.model.topology.NodeTemplate;
import alien4cloud.tosca.container.model.topology.RelationshipTemplate;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.container.services.csar.impl.CSARRepositorySearchService;

import com.google.common.collect.Maps;

@Service
public class TopologyServiceCore {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private CSARRepositorySearchService csarRepoSearchService;

    /**
     * Retrieve a topology given its Id
     *
     * @param topologyId
     * @return
     */
    public Topology getMandatoryTopology(String topologyId) {
        Topology topology = alienDAO.findById(Topology.class, topologyId);
        if (topology == null) {
            throw new NotFoundException("Topology [" + topologyId + "] cannot be found");
        }
        return topology;
    }

    /**
     * Get the Map of {@link NodeTemplate} from a topology
     *
     * @param topology
     * @return
     */
    public Map<String, NodeTemplate> getNodeTemplates(Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        if (nodeTemplates == null) {
            throw new NotFoundException("Topology [" + topology.getId() + "] do not have any node template");
        }
        return nodeTemplates;
    }

    /**
     * Get a {@link NodeTemplate} given its name from a map
     *
     * @param topologyId
     * @param nodeTemplateName
     * @param nodeTemplates
     * @return
     */
    public NodeTemplate getNodeTemplate(String topologyId, String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        NodeTemplate nodeTemplate = nodeTemplates.get(nodeTemplateName);
        if (nodeTemplate == null) {
            throw new NotFoundException("Topology [" + topologyId + "] do not have node template with name [" + nodeTemplateName + "]");
        }
        return nodeTemplate;
    }

    /**
     * Get a {@link NodeTemplate} given its name from a topology
     *
     * @param topology
     * @param nodeTemplateId
     * @return
     */
    public NodeTemplate getNodeTemplate(Topology topology, String nodeTemplateId) {
        Map<String, NodeTemplate> nodeTemplates = getNodeTemplates(topology);
        return getNodeTemplate(topology.getId(), nodeTemplateId, nodeTemplates);
    }

    /**
     * Get the indexed node types used in a topology.
     *
     * @param topology The topology for which to get indexed node types.
     * @param abstractOnly If true, only abstract types will be retrieved.
     * @param useTemplateNameAsKey If true the name of the node template will be used as key for the type in the returned map, if not the type will be used as
     *            key.
     * @return A map of indexed node types.
     */
    public Map<String, IndexedNodeType> getIndexedNodeTypesFromTopology(Topology topology, boolean abstractOnly, boolean useTemplateNameAsKey) {
        Map<String, IndexedNodeType> nodeTypes = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return nodeTypes;
        }
        for (Map.Entry<String, NodeTemplate> template : topology.getNodeTemplates().entrySet()) {
            if (!nodeTypes.containsKey(template.getValue().getType())) {
                IndexedNodeType nodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, template.getValue().getType(),
                        topology.getDependencies());
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
     * @return the map containing rel
     */
    public Map<String, IndexedRelationshipType> getIndexedRelationshipTypesFromTopology(Topology topology) {
        Map<String, IndexedRelationshipType> relationshipTypes = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return relationshipTypes;
        }
        for (Map.Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
            NodeTemplate template = templateEntry.getValue();
            if (template.getRelationships() != null) {
                for (Map.Entry<String, RelationshipTemplate> relationshipEntry : template.getRelationships().entrySet()) {
                    RelationshipTemplate relationship = relationshipEntry.getValue();
                    if (!relationshipTypes.containsKey(relationship.getType())) {
                        IndexedRelationshipType relationshipType = csarRepoSearchService.getRequiredElementInDependencies(IndexedRelationshipType.class,
                                relationship.getType(), topology.getDependencies());
                        relationshipTypes.put(relationship.getType(), relationshipType);
                    }
                }
            }
        }
        return relationshipTypes;
    }
}
