package alien4cloud.topology;

import java.util.*;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositoryIndexerService;
import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TopologyServiceCore {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;
    @Resource
    private CsarService csarService;
    @Resource
    private ICSARRepositoryIndexerService indexerService;

    /**
     * Get the Map of {@link NodeTemplate} from a topology
     *
     * @param topology the topology
     * @return this topology's node templates
     */
    public static Map<String, NodeTemplate> getNodeTemplates(Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        if (nodeTemplates == null) {
            throw new NotFoundException("Topology [" + topology.getId() + "] do not have any node template");
        }
        return nodeTemplates;
    }

    /**
     * Get a {@link NodeTemplate} given its name from a map
     *
     * @param topologyId the topology's id
     * @param nodeTemplateName the name of the node template
     * @param nodeTemplates the topology's node templates
     * @return the found node template, throws NotFoundException if not found
     */
    public static NodeTemplate getNodeTemplate(String topologyId, String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        NodeTemplate nodeTemplate = nodeTemplates.get(nodeTemplateName);
        if (nodeTemplate == null) {
            throw new NotFoundException("Topology [" + topologyId + "] do not have node template with name [" + nodeTemplateName + "]");
        }
        return nodeTemplate;
    }

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
     * Get a {@link NodeTemplate} given its name from a topology
     *
     * @param topology the topology
     * @param nodeTemplateId the name of the node template
     * @return the found node template, throws NotFoundException if not found
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
     * @param failOnTypeNotFound
     * @return A map of indexed node types.
     */
    public Map<String, NodeType> getIndexedNodeTypesFromTopology(Topology topology, boolean abstractOnly, boolean useTemplateNameAsKey,
                                                                 boolean failOnTypeNotFound) {
        return getIndexedNodeTypesFromDependencies(topology.getNodeTemplates(), topology.getDependencies(), abstractOnly, useTemplateNameAsKey,
                failOnTypeNotFound);
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
                                : csarRepoSearchService.getElementInDependencies(RelationshipType.class, relationship.getType(),
                                        topology.getDependencies());
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
                        CapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(CapabilityType.class,
                                capability.getType(), topology.getDependencies());
                        capabilityTypes.put(capability.getType(), capabilityType);
                    }
                }
            }
        }
        return capabilityTypes;
    }

    /**
     * Build a node template
     * 
     * @param dependencies The dependencies in which to search for the node type. This is used by the ToscaContextual annotation.
     * @param indexedNodeType The index node type from which to create the node.
     * @param templateToMerge The node template to merge in the type to create the actual node template.
     * @return return the node template instance.
     */
    @ToscaContextual
    public NodeTemplate buildNodeTemplate(Set<CSARDependency> dependencies, NodeType indexedNodeType, NodeTemplate templateToMerge) {
        return NodeTemplateBuilder.buildNodeTemplate(indexedNodeType, templateToMerge);
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
        topology.setLastUpdateDate(new Date());
        this.alienDAO.save(topology);
    }

    public void updateSubstitutionType(final Topology topology) {
        // FIXME Fix this
//        if (!topology.getDelegateType().equalsIgnoreCase(TopologyTemplate.class.getSimpleName())) {
//            return;
//        }
//        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
//            return;
//        }
//        NodeType nodeType = csarRepoSearchService.getElementInDependencies(NodeType.class,
//                topology.getSubstitutionMapping().getSubstitutionType().getElementId(), topology.getDependencies());
//
//        TopologyTemplate topologyTemplate = alienDAO.findById(TopologyTemplate.class, topology.getDelegateId());
//        TopologyTemplateVersion topologyTemplateVersion = topologyTemplateVersionService.getByTopologyId(topology.getId());
//
//        Set<CSARDependency> inheritanceDependencies = Sets.newHashSet();
//        inheritanceDependencies.add(new CSARDependency(nodeType.getArchiveName(), nodeType.getArchiveVersion()));
//
//        // we have to search for the eventually existing CSar to update it' deps
//        // actually, the csar is not renamed when the topology template is renamed (this is not quite simple to rename a csar if it
//        // is used in topologies ....). So we have to search the csar using the topology id.
//        Csar csar = csarService.getTopologySubstitutionCsar(topology.getId());
//        if (csar == null) {
//            // the csar can not be found, we create it
//            String archiveName = topologyTemplate.getName();
//            String archiveVersion = topologyTemplateVersion.getVersion();
//            csar = new Csar(archiveName, archiveVersion);
//            csar.setSubstitutionTopologyId(topology.getId());
//        }
//        csar.setDependencies(inheritanceDependencies, true);
//        csar.getDependencies().addAll(topology.getDependencies());
//        csar.setImportSource(CSARSource.TOPOLOGY_SUBSTITUTION.name());
//        csar.setHash("-1"); // FIXME manage hash for substitution elements too (should we just generate based on type).
//        csarService.save(csar);
//
//        NodeType topologyTemplateType = new NodeType();
//        topologyTemplateType.setArchiveName(csar.getName());
//        topologyTemplateType.setArchiveVersion(csar.getVersion());
//        topologyTemplateType.setElementId(csar.getName());
//        topologyTemplateType.setDerivedFrom(Lists.newArrayList(nodeType.getElementId()));
//        topologyTemplateType.setSubstitutionTopologyId(topology.getId());
//        List<CapabilityDefinition> capabilities = Lists.newArrayList();
//        topologyTemplateType.setCapabilities(capabilities);
//        List<RequirementDefinition> requirements = Lists.newArrayList();
//        topologyTemplateType.setRequirements(requirements);
//        // inputs from topology become properties of type
//        topologyTemplateType.setProperties(topology.getInputs());
//        // output attributes become attributes for the type
//        Map<String, IValue> attributes = Maps.newHashMap();
//        topologyTemplateType.setAttributes(attributes);
//        Map<String, Set<String>> outputAttributes = topology.getOutputAttributes();
//        if (outputAttributes != null) {
//            for (Entry<String, Set<String>> oae : outputAttributes.entrySet()) {
//                String nodeName = oae.getKey();
//                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
//                NodeType nodeTemplateType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemplate.getType(),
//                        topology.getDependencies());
//                for (String attributeName : oae.getValue()) {
//                    IValue ivalue = nodeTemplateType.getAttributes().get(attributeName);
//                    // we have an issue here : if several nodes have the same attribute name, there is a conflict
//                    if (ivalue != null && !attributes.containsKey(attributeName)) {
//                        attributes.put(attributeName, ivalue);
//                    }
//                }
//            }
//        }
//        // output properties become attributes for the type
//        Map<String, Set<String>> outputProperties = topology.getOutputProperties();
//        if (outputProperties != null) {
//            for (Entry<String, Set<String>> ope : outputProperties.entrySet()) {
//                String nodeName = ope.getKey();
//                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
//                NodeType nodeTemplateType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemplate.getType(),
//                        topology.getDependencies());
//                for (String propertyName : ope.getValue()) {
//                    PropertyDefinition pd = nodeTemplateType.getProperties().get(propertyName);
//                    // we have an issue here : if several nodes have the same attribute name, there is a conflict
//                    if (pd != null && !attributes.containsKey(propertyName)) {
//                        attributes.put(propertyName, pd);
//                    }
//                }
//            }
//        }
//        // output capabilities properties also become attributes for the type
//        Map<String, Map<String, Set<String>>> outputCapabilityProperties = topology.getOutputCapabilityProperties();
//        if (outputCapabilityProperties != null) {
//            for (Entry<String, Map<String, Set<String>>> ocpe : outputCapabilityProperties.entrySet()) {
//                String nodeName = ocpe.getKey();
//                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
//                for (Entry<String, Set<String>> cpe : ocpe.getValue().entrySet()) {
//                    String capabilityName = cpe.getKey();
//                    String capabilityTypeName = nodeTemplate.getCapabilities().get(capabilityName).getType();
//                    CapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(CapabilityType.class,
//                            capabilityTypeName, topology.getDependencies());
//                    for (String propertyName : cpe.getValue()) {
//                        PropertyDefinition pd = capabilityType.getProperties().get(propertyName);
//                        // we have an issue here : if several nodes have the same attribute name, there is a conflict
//                        if (pd != null && !attributes.containsKey(propertyName)) {
//                            attributes.put(propertyName, pd);
//                        }
//                    }
//                }
//            }
//        }
//
//        // capabilities substitution
//        if (topology.getSubstitutionMapping().getCapabilities() != null) {
//            for (Entry<String, SubstitutionTarget> e : topology.getSubstitutionMapping().getCapabilities().entrySet()) {
//                String key = e.getKey();
//                String nodeName = e.getValue().getNodeTemplateName();
//                String capabilityName = e.getValue().getTargetId();
//                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
//                NodeType nodeTemplateType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemplate.getType(),
//                        topology.getDependencies());
//                CapabilityDefinition capabilityDefinition = IndexedModelUtils.getCapabilityDefinitionById(nodeTemplateType.getCapabilities(), capabilityName);
//                capabilityDefinition.setId(key);
//                topologyTemplateType.getCapabilities().add(capabilityDefinition);
//            }
//        }
//        // requirement substitution
//        if (topology.getSubstitutionMapping().getRequirements() != null) {
//            for (Entry<String, SubstitutionTarget> e : topology.getSubstitutionMapping().getRequirements().entrySet()) {
//                String key = e.getKey();
//                String nodeName = e.getValue().getNodeTemplateName();
//                String requirementName = e.getValue().getTargetId();
//                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
//                NodeType nodeTemplateType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemplate.getType(),
//                        topology.getDependencies());
//                RequirementDefinition requirementDefinition = IndexedModelUtils.getRequirementDefinitionById(nodeTemplateType.getRequirements(),
//                        requirementName);
//                requirementDefinition.setId(key);
//                topologyTemplateType.getRequirements().add(requirementDefinition);
//            }
//        }
//        indexerService.indexInheritableElement(csar.getName(), csar.getVersion(), topologyTemplateType, inheritanceDependencies);
    }

}
