package org.alien4cloud.tosca.editor.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.EditorService;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.RebuildNodeOperation;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.DeleteRelationshipOperation;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.RebuildRelationshipOperation;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.utils.AlienUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper service for editor context that allows to get recovery operations based on a given topology.
 */
@Slf4j
@Service
public class EditorTopologyRecoveryHelperService {

    @Inject
    private ICsarService csarService;
    @Inject
    private EditionContextManager editionContextManager;

    @Inject
    private EditorService editorService;

    /**
     * analyse a given topology and build a {@link RecoverTopologyOperation} to apply to the topology to make it synch
     * with the dependencies present in the repository
     * 
     * @param topology
     * @return a {@link RecoverTopologyOperation}
     */
    @ToscaContextual(requiresNew = true)
    public RecoverTopologyOperation buildRecoveryOperation(Topology topology) {
        RecoverTopologyOperation operation = new RecoverTopologyOperation();
        buildRecoveryOperation(topology, operation);
        return CollectionUtils.isNotEmpty(operation.getUpdatedDependencies()) ? operation : null;
    }

    /**
     * analyse a given topology and fill in a {@link RecoverTopologyOperation} to apply to the topology to make it synch
     * with the dependencies present in the repository
     *
     * @param topology
     * @param operation
     */
    @ToscaContextual(requiresNew = true)
    public void buildRecoveryOperation(Topology topology, RecoverTopologyOperation operation) {
        Set<CSARDependency> updatedDependencies = getUpdatedDependencies(topology);
        operation.setUpdatedDependencies(updatedDependencies);
        operation.setRecoveringOperations(buildRecoveryOperations(topology, updatedDependencies));
    }

    /**
     * Given a set of dependencies, analyse a given topology and build a list of {@link AbstractEditorOperation} to apply to the topology to make it synch
     * with the dependencies present in the repository
     *
     * @param topology The topology we want to recover
     * @param updatedDependencies The updated dependencies within the topology
     * @return a list of {@link AbstractEditorOperation} representing the operations to perform on the topology for recovery
     */
    public List<AbstractEditorOperation> buildRecoveryOperations(Topology topology, Set<CSARDependency> updatedDependencies) {
        List<AbstractEditorOperation> recoveryOperations = Lists.newArrayList();
        if (!topology.isEmpty()) {
            for (CSARDependency updatedDependency : AlienUtils.safe(updatedDependencies)) {
                buildNodesRecoveryOperations(topology, updatedDependency, recoveryOperations);
                buildRelationshipsRecoveryOperations(topology, updatedDependency, recoveryOperations);
            }
        }
        return recoveryOperations;
    }

    /**
     *
     * Analyse the relationships and build recovery operations
     * 
     * @param topology The topology to recover
     * @param updatedDependency The updated dependencies within the topology
     * @param recoveryOperations The list into which to add the recovery operations
     */
    private void buildRelationshipsRecoveryOperations(Topology topology, CSARDependency updatedDependency, List<AbstractEditorOperation> recoveryOperations) {
        Set<String> deletedNodes = getDeletedNodes(recoveryOperations);
        for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
            // skip, because the relationships will be deleted with the node
            if (deletedNodes.contains(nodeTemplate.getName())) {
                continue;
            }

            for (RelationshipTemplate relationshipTemplate : AlienUtils.safe(nodeTemplate.getRelationships()).values()) {
                // skip, because the relationships will be deleted with the target node
                if (deletedNodes.contains(relationshipTemplate.getTarget())) {
                    continue;
                }

                RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());

                // this means the type has been deleted. then we should delete the template from the topology
                if (relationshipType == null) {
                    addDeleteRelationshipOperation(nodeTemplate, relationshipTemplate, recoveryOperations);
                    continue;
                }

                // check if the type is from the current archive. if so then we should rebuild the relationship template
                if (isFrom(relationshipType, updatedDependency)) {
                    try {
                        // check if the relationship is still valid, ie the related requirement and capability still exist within the related types
                        validateRelationShip(nodeTemplate, relationshipTemplate, topology);
                        // FIXME instead of systematically rebuilding the relationship, maybe check what really changed in the type before rebuilding it?
                        addRebuildRelationshipOperation(nodeTemplate, relationshipTemplate, recoveryOperations);
                    } catch (Exception e) {
                        // remove the relationship if an error occurs
                        if (log.isDebugEnabled()) {
                            log.debug("Error when trying to recover a relationship", e);
                        }
                        addDeleteRelationshipOperation(nodeTemplate, relationshipTemplate, recoveryOperations);
                    }
                }
            }
        }
    }

    /**
     * checks if a relationship is still valid
     *
     * @param nodeTemplate The source node of the relationship
     * @param relationshipTemplate The relationship to validate
     * @param topology The related topology
     */
    private void validateRelationShip(NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate, Topology topology) {
        // validate that the node itself still has the requirement
        checkRequirement(relationshipTemplate.getRequirementName(), nodeTemplate);
        // validate the targeted capability
        checkCapability(relationshipTemplate.getTargetedCapabilityName(), topology.getNodeTemplates().get(relationshipTemplate.getTarget()));
    }

    /**
     * validate that a node still has the requirement, by checking directly from the related node type
     * 
     * @param requirementName The name of the requirement to check
     * @param nodeTemplate The node template in which to check for the requirement
     */
    private void checkRequirement(String requirementName, NodeTemplate nodeTemplate) {
        // This call should never throw a NotFoundException
        NodeType indexedNodeType = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
        Map<String, RequirementDefinition> requirementMap = AlienUtils.fromListToMap(indexedNodeType.getRequirements(), "id", true);
        if (!AlienUtils.safe(requirementMap).containsKey(requirementName)) {
            throw new NotFoundException("A requirement with name [" + requirementName + "] cannot be found in the node [" + nodeTemplate.getName() + "].");
        }
    }

    /**
     * validate that a node still has a capability, by checkibg directly from the related node type
     *
     * @param capabilityName The name of the capability to check
     * @param nodeTemplate The node template in which to check for the capability
     */
    private void checkCapability(String capabilityName, NodeTemplate nodeTemplate) {
        // This call should never throw a NotFoundException
        NodeType indexedNodeType = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
        Map<String, CapabilityDefinition> capabilitiesMap = AlienUtils.fromListToMap(indexedNodeType.getCapabilities(), "id", true);
        if (!AlienUtils.safe(capabilitiesMap).containsKey(capabilityName)) {
            throw new NotFoundException("A capability with name [" + capabilityName + "] cannot be found in the node [" + nodeTemplate.getName() + "].");
        }
    }

    /**
     * Analyse the node templates and build recovery operations
     *
     * @param topology The topology to recover
     * @param updatedDependency The updated dependencies within the topology
     * @param recoveryOperations The list into which to add the recovery operations
     */
    private void buildNodesRecoveryOperations(Topology topology, CSARDependency updatedDependency, List<AbstractEditorOperation> recoveryOperations) {
        for (NodeTemplate template : topology.getNodeTemplates().values()) {
            NodeType nodeType = ToscaContext.get(NodeType.class, template.getType());

            // this means the type has been deleted. then we should delete the template from the topology
            if (nodeType == null) {
                addDeleteNodeOperation(template, recoveryOperations);
                continue;
            }

            // check if the type is from the current archive. if so then we should rebuild the nodeTemplate
            if (isFrom(nodeType, updatedDependency)) {
                // FIXME instead of systematically rebuilding the node, maybe check what really changed in the nodeType before rebuilding it?
                addRebuildNodeOperation(template, recoveryOperations);
            }
        }
    }

    private void addRebuildNodeOperation(NodeTemplate template, List<AbstractEditorOperation> recoveryOperations) {
        RebuildNodeOperation operation = new RebuildNodeOperation();
        operation.setNodeName(template.getName());
        recoveryOperations.add(operation);
    }

    private void addDeleteNodeOperation(NodeTemplate template, List<AbstractEditorOperation> recoveryOperations) {
        DeleteNodeOperation operation = new DeleteNodeOperation();
        operation.setNodeName(template.getName());
        recoveryOperations.add(operation);
    }

    private void addRebuildRelationshipOperation(NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate,
            List<AbstractEditorOperation> recoveryOperations) {
        RebuildRelationshipOperation operation = new RebuildRelationshipOperation();
        operation.setNodeName(nodeTemplate.getName());
        operation.setRelationshipName(relationshipTemplate.getName());
        recoveryOperations.add(operation);
    }

    private void addDeleteRelationshipOperation(NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate,
            List<AbstractEditorOperation> recoveryOperations) {
        DeleteRelationshipOperation operation = new DeleteRelationshipOperation();
        operation.setNodeName(nodeTemplate.getName());
        operation.setRelationshipName(relationshipTemplate.getName());
        recoveryOperations.add(operation);
    }

    /**
     * Return a Set of updated {@link CSARDependency} that have changed since last added in a given topology
     *
     * @param topology
     * @return
     */
    public Set<CSARDependency> getUpdatedDependencies(Topology topology) {
        Set<CSARDependency> dependencies = topology.getDependencies();
        Set<CSARDependency> updatedDependencies = Sets.newHashSet();
        for (CSARDependency csarDependency : dependencies) {
            CSARDependency updatedDependency = getUpdatedDependencyIfNeeded(csarDependency);
            if (updatedDependency != null) {
                updatedDependencies.add(updatedDependency);
            }
        }
        return updatedDependencies;
    }

    /**
     * Update a dependency according to what is currently in the repository
     *
     * @param initialDependency
     * @return
     */
    private CSARDependency getUpdatedDependencyIfNeeded(CSARDependency initialDependency) {
        CSARDependency updatedDependency = null;
        Csar csar = csarService.getOrFail(initialDependency.getName(), initialDependency.getVersion());
        if ((StringUtils.isNotBlank(initialDependency.getHash()) || StringUtils.isNotBlank(csar.getHash()))
                && !Objects.equals(initialDependency.getHash(), csar.getHash())) {
            updatedDependency = new CSARDependency(csar.getName(), csar.getVersion(), csar.getHash());
        }
        return updatedDependency;
    }

    private boolean isFrom(AbstractToscaType element, CSARDependency dependency) {
        return Objects.equals(element.getArchiveName(), dependency.getName()) /*&& Objects.equals(element.getArchiveVersion(), dependency.getVersion())*/;
    }

    /**
     * Get the names of the nodes to be deleted according to the recovery operations
     * 
     * @param recoveryOperations
     * @return A set containing the names of the to-be-deleted nodes
     */
    private Set<String> getDeletedNodes(List<AbstractEditorOperation> recoveryOperations) {
        Set<String> names = Sets.newHashSet();
        for (AbstractEditorOperation operation : AlienUtils.safe(recoveryOperations)) {
            if (operation instanceof DeleteNodeOperation) {
                names.add(((DeleteNodeOperation) operation).getNodeName());
            }
        }
        return names;
    }

    @ToscaContextual(requiresNew = true)
    public void processRecoveryOperations(Topology topology, Collection<AbstractEditorOperation> operations) {
        for (AbstractEditorOperation recoveryOperation : AlienUtils.safe(operations)) {
            editorService.process(recoveryOperation);
        }
    }
}
