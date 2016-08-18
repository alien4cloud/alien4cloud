package alien4cloud.topology;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.component.IToscaElementFinder;
import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.topology.validation.TopologyCapabilityBoundsValidationServices;
import alien4cloud.topology.validation.TopologyRequirementBoundsValidationServices;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TopologyRecoveryService {

    @Inject
    private CsarService csarService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private TopologyService topologyService;
    @Inject
    private TopologyCapabilityBoundsValidationServices capabilityBoundsValidationServices;
    @Inject
    private TopologyRequirementBoundsValidationServices requirementBoundsValidationServices;
    @Inject
    private ICSARRepositorySearchService csarRepoSearchService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;

    /**
     * Return a Set of {@link CSARDependency} that have changed since last added in a given topology
     * 
     * @param topology
     * @return
     */
    public Set<CSARDependency> getUpdatedDependencies(Topology topology) {
        Set<CSARDependency> dependencies = topology.getDependencies();
        Set<CSARDependency> modifiedDependencies = Sets.newHashSet();
        for (CSARDependency csarDependency : dependencies) {
            if (hasChangedSinceLastAddedIntoTopology(csarDependency)) {
                modifiedDependencies.add(csarDependency);
            }
        }
        return modifiedDependencies;
    }

    private boolean hasChangedSinceLastAddedIntoTopology(CSARDependency dependency) {
        Csar csar = csarService.getIfExists(dependency.getName(), dependency.getVersion());
        return (StringUtils.isNotBlank(dependency.getHash()) || StringUtils.isNotBlank(csar.getHash()))
                && !Objects.equals(dependency.getHash(), csar.getHash());
    }

    /**
     * Synchronize the topology with the existing element in the database of a set of csar dependencies
     * 
     * @param updatedDependencies The dependencies representing the archives that have been updated since added into the topology
     * @param topology The topology to synchronize
     */
    @ToscaContextual
    public void recoverTopology(Collection<CSARDependency> updatedDependencies, Topology topology) {
        // do not process if the topology is empty
        if (topology.isEmpty()) {
            return;
        }

        TopologyContext context = workflowBuilderService.buildTopologyContext(topology);
        for (CSARDependency updatedDependency : updatedDependencies) {
            recoverNodeTemplates(topology, updatedDependency, context);
            recoverRelationships(topology, updatedDependency, context);
        }

        saveAndUpdate(topology);
    }

    private void saveAndUpdate(Topology topology) {
        topologyService.rebuildDependencies(topology);
        topologyServiceCore.save(topology);
        topologyServiceCore.updateSubstitutionType(topology);
    }

    private void recoverNodeTemplates(Topology topology, CSARDependency updatedDependency, TopologyContext context) {
        // Map<String, IndexedNodeType> nodeTypes = topologiServiceCore.getIndexedNodeTypesFromTopology(topology, false, true);
        Set<String> templateNames = Sets.newHashSet(topology.getNodeTemplates().keySet());
        Set<NodeTemplate> removed = Sets.newHashSet();

        for (String nodeTemplateName : templateNames) {
            NodeTemplate template = topology.getNodeTemplates().get(nodeTemplateName);
            template.setName(nodeTemplateName);
            IndexedNodeType nodeType = context.findElement(IndexedNodeType.class, template.getType());

            // this means the type has been deleted. then delete the template from the topology
            if (nodeType == null) {
                removeNodeTemplate(template, topology);
                removed.add(template);
                continue;
            }

            // check if the type is from the current archive. if so then update the nodeTemplate
            if (isFrom(nodeType, updatedDependency)) {
                NodeTemplate rebuiltNodeTemplate = NodeTemplateBuilder.buildNodeTemplate(nodeType, template);
                rebuiltNodeTemplate.setName(nodeTemplateName);
                topology.getNodeTemplates().put(nodeTemplateName, rebuiltNodeTemplate);
            }
        }

        // clean wfs
        for (NodeTemplate nodeTemplate : removed) {
            workflowBuilderService.removeNode(topology, nodeTemplate.getName(), nodeTemplate);
        }

    }

    private void removeNodeTemplate(NodeTemplate value, Topology topology) {
        topologyService.cleanArtifactsFromRepository(value);
        topologyService.removeAndCleanTopology(value, topology);
    }

    private void recoverRelationships(Topology topology, CSARDependency updatedDependency, TopologyContext context) {
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        Map<String, Map<String, RelationshipTemplate>> removed = Maps.newHashMap();
        for (Entry<String, NodeTemplate> entry : nodeTemplates.entrySet()) {
            NodeTemplate nodeTemplate = entry.getValue();
            if (MapUtils.isEmpty(nodeTemplate.getRelationships())) {
                continue;
            }
            Map<String, RelationshipTemplate> relRemoved = Maps.newHashMap();
            removed.put(entry.getKey(), relRemoved);
            Set<String> relationshipNames = Sets.newHashSet(nodeTemplate.getRelationships().keySet());
            // while (relationshipsIter.hasNext()) {
            for (String relationshipName : relationshipNames) {
                RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipName);
                IndexedRelationshipType relationshipType = context.findElement(IndexedRelationshipType.class, relationshipTemplate.getType());

                // this means the type has been deleted. then delete the template from the topology
                if (relationshipType == null) {
                    nodeTemplate.getRelationships().remove(relationshipName);
                    relRemoved.put(relationshipName, relationshipTemplate);
                    continue;
                }

                // check if the type is from the current archive. if so then update the template
                if (isFrom(relationshipType, updatedDependency)) {
                    // we remove it first, for bound calculation
                    nodeTemplate.getRelationships().remove(relationshipName);
                    relRemoved.put(relationshipName, relationshipTemplate);
                    boolean isSynchronized = tryRecoverRelationship(nodeTemplate, relationshipType, relationshipName, relationshipTemplate, topology, context);
                    // if able to synch, then re-add it into the map
                    if (isSynchronized) {
                        nodeTemplate.getRelationships().put(relationshipName, relationshipTemplate);
                        relRemoved.remove(relationshipName);
                    }
                }
            }

            if (MapUtils.isEmpty(removed.get(entry.getKey()))) {
                removed.remove(entry.getKey());
            }
        }

        // clean wfs
        for (Entry<String, Map<String, RelationshipTemplate>> entry : removed.entrySet()) {
            Map<String, RelationshipTemplate> relRemoved = entry.getValue();
            for (Entry<String, RelationshipTemplate> relEntry : relRemoved.entrySet()) {
                workflowBuilderService.removeRelationship(topology, entry.getKey(), relEntry.getKey(), relEntry.getValue());
            }
        }
    }

    /**
     * try to synch a relationship.
     * 
     * @param relType
     * @param key
     * @param value
     * @param topology
     * @param context
     * @return false if it is synchronized, false if not, meaning we should remove it
     */
    private boolean tryRecoverRelationship(NodeTemplate nodeTemplate, IndexedRelationshipType relType, String relationshipName,
            RelationshipTemplate relationshipTemplate, Topology topology, final TopologyContext context) {
        try {
            // try validation bounds
            if (requirementBoundsValidationServices.isRequirementUpperBoundReachedForSource(nodeTemplate, relationshipTemplate.getRequirementName(),
                    new ContextToscaElementFinder(context))) {
                return false;
            }

            if (capabilityBoundsValidationServices.isCapabilityUpperBoundReachedForTarget(relationshipTemplate.getTarget(), topology.getNodeTemplates(),
                    relationshipTemplate.getTargetedCapabilityName(), new ContextToscaElementFinder(context))) {
                return false;
            }

            // rebuild a relationship template based on the current relationship type
            Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
            NodeTemplateBuilder.fillProperties(properties, relType.getProperties(), relationshipTemplate.getProperties());
            relationshipTemplate.setProperties(properties);
            relationshipTemplate.setAttributes(relType.getAttributes());
        } catch (Exception e) {
            log.debug("Error when trying to synch a relationship", e);
            return false;
        }
        return true;
    }

    private boolean isFrom(IndexedToscaElement element, CSARDependency dependency) {
        return Objects.equals(element.getArchiveName(), dependency.getName()) && Objects.equals(element.getArchiveVersion(), dependency.getVersion());
    }

    @AllArgsConstructor
    private class ContextToscaElementFinder implements IToscaElementFinder {
        TopologyContext context;

        @Override
        public <T extends IndexedToscaElement> T findElement(Class<T> elementClass, String elementId) {
            return context.findElement(elementClass, elementId);
        }

    }
}
