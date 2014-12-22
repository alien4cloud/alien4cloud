package alien4cloud.paas.plan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import lombok.SneakyThrows;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedRelationshipType;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.exception.InvalidTopologyException;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.container.model.NormativeBlockStorageConstants;
import alien4cloud.tosca.container.model.NormativeComputeConstants;
import alien4cloud.tosca.container.model.NormativeNetworkConstants;
import alien4cloud.tosca.container.model.NormativeRelationshipConstants;
import alien4cloud.tosca.container.model.topology.AbstractTemplate;
import alien4cloud.tosca.container.model.topology.NodeTemplate;
import alien4cloud.tosca.container.model.topology.RelationshipTemplate;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.container.services.csar.impl.CSARRepositorySearchService;
import alien4cloud.utils.TypeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Utility to build an hosted on tree from a topology.
 */
@Component
public class TopologyTreeBuilderService {
    @Resource
    private CsarFileRepository repository;
    @Resource
    private CSARRepositorySearchService csarSearchService;

    /**
     * Fetch informations from the repository to complete the topology node template informations with additional data such as artifacts paths etc.
     *
     * @param topology The topology for which to build PaaSNodeTemplate map.
     * @return A map of PaaSNodeTemplate that match the one of the NodeTempaltes in the given topology (and filled with artifact paths etc.).
     */
    public Map<String, PaaSNodeTemplate> buildPaaSNodeTemplate(Topology topology) {
        Map<String, PaaSNodeTemplate> nodeTemplates = Maps.newHashMap();

        // cache IndexedToscaElements, CloudServiceArchive and ToscaElements to limit queries.
        TypeMap cache = new TypeMap();

        // Fill in PaaSNodeTemplate by fetching node types and CSAR path from the repositories.
        if (topology.getNodeTemplates() != null) {
            for (Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
                NodeTemplate template = templateEntry.getValue();

                PaaSNodeTemplate paaSNodeTemplate = new PaaSNodeTemplate(templateEntry.getKey(), template);

                fillType(cache, topology, template, paaSNodeTemplate, IndexedNodeType.class);

                if (template.getRelationships() != null) {
                    for (Map.Entry<String, RelationshipTemplate> relationshipEntry : template.getRelationships().entrySet()) {
                        RelationshipTemplate relationshipTemplate = relationshipEntry.getValue();
                        PaaSRelationshipTemplate paaSRelationshipTemplate = new PaaSRelationshipTemplate(relationshipEntry.getKey(), relationshipTemplate,
                                paaSNodeTemplate.getId());
                        fillType(cache, topology, relationshipTemplate, paaSRelationshipTemplate, IndexedRelationshipType.class);
                        paaSNodeTemplate.getRelationshipTemplates().add(paaSRelationshipTemplate);
                    }
                }

                if (topology.getScalingPolicies() != null) {
                    paaSNodeTemplate.setScalingPolicy(topology.getScalingPolicies().get(templateEntry.getKey()));
                }

                nodeTemplates.put(templateEntry.getKey(), paaSNodeTemplate);
            }
        }
        return nodeTemplates;
    }

    /**
     * Build a tree of the topology nodes based on hosted on relationships.
     *
     * @param nodeTemplates The node templates that are part of the topology.
     * @return A list of root nodes on the topology hosted on tree.
     */
    public List<PaaSNodeTemplate> getHostedOnTree(Map<String, PaaSNodeTemplate> nodeTemplates) {
        // Build hosted_on tree
        List<PaaSNodeTemplate> roots = new ArrayList<PaaSNodeTemplate>();
        for (Entry<String, PaaSNodeTemplate> entry : nodeTemplates.entrySet()) {
            PaaSNodeTemplate paaSNodeTemplate = entry.getValue();
            boolean isCompute = ToscaUtils.isFromType(NormativeComputeConstants.COMPUTE_TYPE, paaSNodeTemplate.getIndexedNodeType());
            boolean isNetwork = ToscaUtils.isFromType(NormativeNetworkConstants.NETWORK_TYPE, paaSNodeTemplate.getIndexedNodeType());

            // manage block storage
            if (ToscaUtils.isFromType(NormativeBlockStorageConstants.BLOCKSTORAGE_TYPE, paaSNodeTemplate.getIndexedNodeType())) {
                processBlockStorage(paaSNodeTemplate, nodeTemplates);
                continue;
            }

            // manage network
            if (isCompute) {
                processNetwork(paaSNodeTemplate, nodeTemplates);
            }

            // do nothing special for network nodes. We should manage them in the workflow later on.
            if (!isNetwork) {
                PaaSRelationshipTemplate hostedOnRelationship = getPaaSRelationshipTemplateFromType(paaSNodeTemplate, NormativeRelationshipConstants.HOSTED_ON);
                if (hostedOnRelationship == null) {
                    roots.add(paaSNodeTemplate);
                } else {
                    String target = hostedOnRelationship.getRelationshipTemplate().getTarget();
                    PaaSNodeTemplate parent = nodeTemplates.get(target);
                    parent.getChildren().add(paaSNodeTemplate);
                    paaSNodeTemplate.setParent(parent);
                    if (hostedOnRelationship.instanceOf(NormativeRelationshipConstants.SEQUENCE_HOSTED_ON)) {
                        parent.setCreateChildrenSequence(true);
                    }
                }
            }

            // Relationships are defined from sources to target. We have to make sure that target node also has the relationship injected.
            List<PaaSRelationshipTemplate> allRelationships = getPaaSRelationshipsTemplateFromType(paaSNodeTemplate, NormativeRelationshipConstants.ROOT);
            for (PaaSRelationshipTemplate relationship : allRelationships) {
                // inject the relationship in it's target.
                String target = relationship.getRelationshipTemplate().getTarget();
                nodeTemplates.get(target).getRelationshipTemplates().add(relationship);
            }
        }

        return roots;
    }

    private void processNetwork(PaaSNodeTemplate paaSNodeTemplate, Map<String, PaaSNodeTemplate> nodeTemplates) {
        PaaSRelationshipTemplate networkRelationship = getPaaSRelationshipTemplateFromType(paaSNodeTemplate, NormativeRelationshipConstants.NETWORK);
        if (networkRelationship != null) {
            String target = networkRelationship.getRelationshipTemplate().getTarget();
            PaaSNodeTemplate network = nodeTemplates.get(target);
            paaSNodeTemplate.setNetworkNode(network);
            network.setParent(paaSNodeTemplate);
        }
    }

    private void processBlockStorage(PaaSNodeTemplate paaSNodeTemplate, Map<String, PaaSNodeTemplate> nodeTemplates) {
        PaaSRelationshipTemplate attachTo = getPaaSRelationshipTemplateFromType(paaSNodeTemplate, NormativeRelationshipConstants.ATTACH_TO);
        if (attachTo != null) {
            String target = attachTo.getRelationshipTemplate().getTarget();
            PaaSNodeTemplate parent = nodeTemplates.get(target);
            parent.setAttachedNode(paaSNodeTemplate);
            paaSNodeTemplate.setParent(parent);
        }
    }

    /**
     * Get a single relationship from a given type. Note that the relationship MUST be unique, if not we throw an exception as the workflow cannot be generated.
     *
     * @param paaSNodeTemplate The node for which to get relationship.
     * @param type The type of relationship.
     * @return The unique relationship that matches the given type.
     */
    private PaaSRelationshipTemplate getPaaSRelationshipTemplateFromType(PaaSNodeTemplate paaSNodeTemplate, String type) {
        List<PaaSRelationshipTemplate> relationships = getPaaSRelationshipsTemplateFromType(paaSNodeTemplate, type);
        if (relationships.size() == 1) {
            return relationships.get(0);
        }
        if (relationships.size() > 1) {
            throw new InvalidTopologyException("Relationship that extends <" + type + "> must be unique on a given node.");
        }
        return null;
    }

    /**
     * Get all relationships from a given type (only if the node is the source of the relationship).
     * 
     * @param paaSNodeTemplate The node.
     * @param type The type of relationships to get.
     * @return The relationship template
     */
    private List<PaaSRelationshipTemplate> getPaaSRelationshipsTemplateFromType(PaaSNodeTemplate paaSNodeTemplate, String type) {
        List<PaaSRelationshipTemplate> relationships = Lists.newArrayList();
        for (PaaSRelationshipTemplate relationship : paaSNodeTemplate.getRelationshipTemplates()) {
            if (relationship.instanceOf(type) && relationship.getSource().equals(paaSNodeTemplate.getId())) {
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows({ CSARVersionNotFoundException.class })
    private <V extends IndexedToscaElement> void fillType(TypeMap typeMap, Topology topology, AbstractTemplate template, IPaaSTemplate<V> paaSTemplate,
            Class<? extends IndexedToscaElement> clazz) {
        IndexedToscaElement indexedToscaElement = typeMap.get(clazz, template.getType());
        if (indexedToscaElement == null) {
            indexedToscaElement = csarSearchService.getElementInDependencies(clazz, template.getType(), topology.getDependencies());
            if (indexedToscaElement == null) {
                throw new NotFoundException("Type <" + template.getType() + "> required in the topology cannot be found in the repository.");
            }
            typeMap.put(template.getType(), indexedToscaElement);
        }
        paaSTemplate.setIndexedToscaElement((V) indexedToscaElement);
        Path csarPath = repository.getCSAR(indexedToscaElement.getArchiveName(), indexedToscaElement.getArchiveVersion());
        paaSTemplate.setCsarPath(csarPath);
    }

}
