package alien4cloud.topology;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.component.IToscaElementFinder;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IAttributeValue;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Requirement;
import alien4cloud.model.topology.Topology;
import alien4cloud.utils.PropertyUtil;

import com.google.common.collect.Maps;

@Service
public class TopologyServiceCore {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    /**
     * The default tosca element finder will search into repo.
     */
    private IToscaElementFinder repoToscaElementFinder = new IToscaElementFinder() {
        @Override
        public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies) {
            return csarRepoSearchService.getElementInDependencies(elementClass, elementId, dependencies);
        }
    };

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

    /**
     * Get IndexedRelationshipType in a topology
     *
     * @param topology the topology to find all relationship types
     * @return the map containing rel
     */
    public Map<String, IndexedCapabilityType> getIndexedCapabilityTypesFromTopology(Topology topology) {
        Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return capabilityTypes;
        }
        for (Map.Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
            NodeTemplate template = templateEntry.getValue();
            if (template.getCapabilities() != null) {
                for (Map.Entry<String, Capability> capabilityEntry : template.getCapabilities().entrySet()) {
                    Capability capability = capabilityEntry.getValue();
                    if (!capabilityTypes.containsKey(capability.getType())) {
                        IndexedCapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                                capability.getType(), topology.getDependencies());
                        capabilityTypes.put(capability.getType(), capabilityType);
                    }
                }
            }
        }
        return capabilityTypes;
    }

    public IndexedNodeType getRelatedIndexedNodeType(NodeTemplate nodeTemplate, Topology topology) {
        return csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(), topology.getDependencies());
    }

    public NodeTemplate buildNodeTemplate(Set<CSARDependency> dependencies, IndexedNodeType indexedNodeType, NodeTemplate templateToMerge) {
        return buildNodeTemplate(dependencies, indexedNodeType, templateToMerge, repoToscaElementFinder);
    }

    /**
     * Build a node template
     *
     * @param dependencies the dependencies on which new node will be constructed
     * @param indexedNodeType the type of the node
     * @param templateToMerge the template that can be used to merge into the new node template
     * @return new constructed node template
     */
    public static NodeTemplate buildNodeTemplate(Set<CSARDependency> dependencies, IndexedNodeType indexedNodeType, NodeTemplate templateToMerge,
            IToscaElementFinder toscaElementFinder) {
        NodeTemplate nodeTemplate = new NodeTemplate();
        nodeTemplate.setType(indexedNodeType.getElementId());
        Map<String, Capability> capabilities = Maps.newLinkedHashMap();
        Map<String, Requirement> requirements = Maps.newLinkedHashMap();
        Map<String, AbstractPropertyValue> properties = Maps.newLinkedHashMap();
        Map<String, String> attributes = Maps.newLinkedHashMap();
        Map<String, DeploymentArtifact> deploymentArtifacts = null;
        Map<String, DeploymentArtifact> deploymentArtifactsToMerge = templateToMerge != null ? templateToMerge.getArtifacts() : null;
        if (deploymentArtifactsToMerge != null) {
            if (indexedNodeType.getArtifacts() != null) {
                deploymentArtifacts = Maps.newLinkedHashMap(indexedNodeType.getArtifacts());
                for (Entry<String, DeploymentArtifact> entryArtifact : deploymentArtifactsToMerge.entrySet()) {
                    DeploymentArtifact existingArtifact = entryArtifact.getValue();
                    if (deploymentArtifacts.containsKey(entryArtifact.getKey())) {
                        deploymentArtifacts.put(entryArtifact.getKey(), existingArtifact);
                    }
                }
            }
        } else if (indexedNodeType.getArtifacts() != null) {
            deploymentArtifacts = Maps.newLinkedHashMap(indexedNodeType.getArtifacts());
        }
        fillCapabilitiesMap(capabilities, indexedNodeType.getCapabilities(), dependencies, templateToMerge != null ? templateToMerge.getCapabilities() : null,
                toscaElementFinder);
        fillRequirementsMap(requirements, indexedNodeType.getRequirements(), dependencies, templateToMerge != null ? templateToMerge.getRequirements() : null,
                toscaElementFinder);
        fillProperties(properties, indexedNodeType.getProperties(), templateToMerge != null ? templateToMerge.getProperties() : null);
        fillAttributes(attributes, indexedNodeType.getAttributes());

        nodeTemplate.setCapabilities(capabilities);
        nodeTemplate.setRequirements(requirements);
        nodeTemplate.setProperties(properties);
        nodeTemplate.setAttributes(attributes);
        nodeTemplate.setArtifacts(deploymentArtifacts);
        if (templateToMerge != null && templateToMerge.getRelationships() != null) {
            nodeTemplate.setRelationships(templateToMerge.getRelationships());
        }
        return nodeTemplate;
    }

    private static void fillAttributes(Map<String, String> attributes, Map<String, IAttributeValue> attributes2) {
        if (attributes2 == null || attributes == null) {
            return;
        }
        for (Entry<String, IAttributeValue> entry : attributes2.entrySet()) {
            attributes.put(entry.getKey(), null);
        }
    }

    public static void fillProperties(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> propertiesDefinitions,
            Map<String, AbstractPropertyValue> map) {
        if (propertiesDefinitions == null || properties == null) {
            return;
        }
        for (Map.Entry<String, PropertyDefinition> entry : propertiesDefinitions.entrySet()) {
            AbstractPropertyValue existingValue = MapUtils.getObject(map, entry.getKey());
            if (existingValue == null) {
                String defaultValue = entry.getValue().getDefault();
                if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                    properties.put(entry.getKey(), new ScalarPropertyValue(defaultValue));
                } else {
                    properties.put(entry.getKey(), null);
                }
            } else {
                properties.put(entry.getKey(), existingValue);
            }
        }
    }

    private static void fillCapabilitiesMap(Map<String, Capability> map, List<CapabilityDefinition> elements, Collection<CSARDependency> dependencies,
            Map<String, Capability> mapToMerge, IToscaElementFinder toscaElementFinder) {
        if (elements == null) {
            return;
        }
        for (CapabilityDefinition capa : elements) {
            Capability toAddCapa = MapUtils.getObject(mapToMerge, capa.getId());
            if (toAddCapa == null) {
                toAddCapa = new Capability();
                toAddCapa.setType(capa.getType());
                IndexedCapabilityType indexedCapa = toscaElementFinder.getElementInDependencies(IndexedCapabilityType.class, capa.getType(), dependencies);
                if (indexedCapa != null && indexedCapa.getProperties() != null) {
                    toAddCapa.setProperties(PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(indexedCapa.getProperties()));
                }
            }
            map.put(capa.getId(), toAddCapa);
        }
    }

    private static void fillRequirementsMap(Map<String, Requirement> map, List<RequirementDefinition> elements, Collection<CSARDependency> dependencies,
            Map<String, Requirement> mapToMerge, IToscaElementFinder toscaElementFinder) {
        if (elements == null) {
            return;
        }
        for (RequirementDefinition requirement : elements) {
            Requirement toAddRequirement = MapUtils.getObject(mapToMerge, requirement.getId());
            if (toAddRequirement == null) {
                toAddRequirement = new Requirement();
                toAddRequirement.setType(requirement.getType());
                IndexedCapabilityType indexedReq = toscaElementFinder
                        .getElementInDependencies(IndexedCapabilityType.class, requirement.getType(), dependencies);
                if (indexedReq != null && indexedReq.getProperties() != null) {
                    toAddRequirement.setProperties(PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(indexedReq.getProperties()));
                }
            }
            map.put(requirement.getId(), toAddRequirement);
        }
    }

    public TopologyTemplate createTopologyTemplate(Topology topology, String name, String description) {
        String topologyId = UUID.randomUUID().toString();
        topology.setId(topologyId);

        String topologyTemplateId = UUID.randomUUID().toString();
        TopologyTemplate topologyTemplate = new TopologyTemplate();
        topologyTemplate.setId(topologyTemplateId);
        topologyTemplate.setName(name);
        topologyTemplate.setDescription(description);
        topologyTemplate.setTopologyId(topologyId);

        topology.setDelegateId(topologyTemplateId);
        topology.setDelegateType(TopologyTemplate.class.getSimpleName().toLowerCase());

        this.alienDAO.save(topology);
        this.alienDAO.save(topologyTemplate);

        return topologyTemplate;

    }

    public String ensureNameUnicity(String name, int attemptCount) {
        String computedName = name;
        if (attemptCount > 0) {
            computedName += "-" + attemptCount;
        }
        if (alienDAO.count(TopologyTemplate.class, QueryBuilders.termQuery("name", computedName)) > 0) {
            return ensureNameUnicity(name, ++attemptCount);
        }
        return computedName;
    }

}
