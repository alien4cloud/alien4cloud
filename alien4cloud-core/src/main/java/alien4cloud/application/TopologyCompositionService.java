package alien4cloud.application;

import static alien4cloud.paas.function.FunctionEvaluator.isGetInput;

import java.util.*;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.CyclicReferenceException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.*;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TopologyCompositionService {

    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    public void processTopologyComposition(Topology topology) {
        Deque<CompositionCouple> stack = new ArrayDeque<CompositionCouple>();
        recursivelyBuildSubstitutionStack(topology, stack, "");
        // now this stack contains all the embedded topology templates
        if (!stack.isEmpty()) {
            // iterate over the stack in descending order (manage the deepest topologies at a first time).
            Iterator<CompositionCouple> compositionIterator = stack.descendingIterator();
            while (compositionIterator.hasNext()) {
                processComposition(compositionIterator.next());
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Topology composition has been processed for topology <%s> substituting %d embeded topologies", topology.getId(),
                        stack.size()));
            }

            // std workflows are reinitialized when some composition is processed
            // TODO: find a better way to manage this
            TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);
            workflowBuilderService.reinitWorkflow(Workflow.INSTALL_WF, topologyContext);
            workflowBuilderService.reinitWorkflow(Workflow.UNINSTALL_WF, topologyContext);

        }
    }

    /**
     * Process the composition:
     * <ul>
     * <li>remove the 'proxy' node from the parent.
     * <li>merge the child topology nodes into the parent nodes.
     * <li>
     * </ul>
     * 
     * @param compositionCouple
     */
    private void processComposition(CompositionCouple compositionCouple) {
        // first of all, remove the proxy node from the parent
        NodeTemplate proxyNodeTemplate = compositionCouple.parent.getNodeTemplates().remove(compositionCouple.nodeName);
        // properties of the proxy are used to feed the property values of child node that use get_input
        for (NodeTemplate childNodeTemplate : compositionCouple.child.getNodeTemplates().values()) {
            for (Entry<String, AbstractPropertyValue> propertyEntry : childNodeTemplate.getProperties().entrySet()) {
                AbstractPropertyValue pValue = propertyEntry.getValue();
                if (isGetInput(pValue)) {
                    String inputName = ((FunctionPropertyValue) pValue).getTemplateName();
                    propertyEntry.setValue(proxyNodeTemplate.getProperties().get(inputName));
                }
            }
            for (Entry<String, Capability> capabilityEntry : childNodeTemplate.getCapabilities().entrySet()) {
                if (capabilityEntry.getValue().getProperties() != null) {
                    for (Entry<String, AbstractPropertyValue> propertyEntry : capabilityEntry.getValue().getProperties().entrySet()) {
                        AbstractPropertyValue pValue = propertyEntry.getValue();
                        if (isGetInput(pValue)) {
                            String inputName = ((FunctionPropertyValue) pValue).getTemplateName();
                            propertyEntry.setValue(proxyNodeTemplate.getProperties().get(inputName));
                        }
                    }
                }
            }
        }
        // all relations from the proxy must now start from the corresponding node
        if (proxyNodeTemplate.getRelationships() != null) {
            for (Entry<String, RelationshipTemplate> e : proxyNodeTemplate.getRelationships().entrySet()) {
                String relationShipKey = e.getKey();
                RelationshipTemplate proxyRelationShip = e.getValue();
                String requirementName = proxyRelationShip.getRequirementName();
                SubstitutionTarget substitutionTarget = compositionCouple.child.getSubstitutionMapping().getRequirements().get(requirementName);
                NodeTemplate nodeTemplate = compositionCouple.child.getNodeTemplates().get(substitutionTarget.getNodeTemplateName());
                if (nodeTemplate.getRelationships() == null) {
                    Map<String, RelationshipTemplate> relationships = Maps.newHashMap();
                    nodeTemplate.setRelationships(relationships);
                }
                nodeTemplate.getRelationships().put(relationShipKey, proxyRelationShip);
                proxyRelationShip.setRequirementName(substitutionTarget.getTargetId());
            }
        }
        // all relations that target the proxy must be redirected to the corresponding child node
        for (NodeTemplate otherNodes : compositionCouple.parent.getNodeTemplates().values()) {
            if (otherNodes.getRelationships() != null) {
                for (RelationshipTemplate relationshipTemplate : otherNodes.getRelationships().values()) {
                    if (relationshipTemplate.getTarget().equals(compositionCouple.nodeName)) {
                        SubstitutionTarget st = compositionCouple.child.getSubstitutionMapping().getCapabilities()
                                .get(relationshipTemplate.getTargetedCapabilityName());
                        relationshipTemplate.setTarget(st.getNodeTemplateName());
                        relationshipTemplate.setTargetedCapabilityName(st.getTargetId());
                    }
                }
            }
        }
        if (compositionCouple.parent.getOutputAttributes() != null) {
            Set<String> outputAttributes = compositionCouple.parent.getOutputAttributes().remove(compositionCouple.nodeName);
            if (outputAttributes != null) {
                for (String proxyAttributeName : outputAttributes) {
                    sustituteGetAttribute(compositionCouple.child, compositionCouple.parent, proxyAttributeName);
                }
            }
        }
        // the parent itself expose stuffs, we eventually need to replace substitution targets
        if (compositionCouple.parent.getSubstitutionMapping() != null) {
            if (compositionCouple.parent.getSubstitutionMapping().getCapabilities() != null) {
                for (Entry<String, SubstitutionTarget> substitutionCapabilityEntry : compositionCouple.parent.getSubstitutionMapping().getCapabilities()
                        .entrySet()) {
                    if (substitutionCapabilityEntry.getValue().getNodeTemplateName().equals(compositionCouple.nodeName)) {
                        String targetCapability = substitutionCapabilityEntry.getValue().getTargetId();
                        // just substitute the substitution target
                        substitutionCapabilityEntry.setValue(compositionCouple.child.getSubstitutionMapping().getCapabilities().get(targetCapability));
                    }
                }
            }
            if (compositionCouple.parent.getSubstitutionMapping().getRequirements() != null) {
                for (Entry<String, SubstitutionTarget> e : compositionCouple.parent.getSubstitutionMapping().getRequirements().entrySet()) {
                    if (e.getValue().getNodeTemplateName().equals(compositionCouple.nodeName)) {
                        String targetCapability = e.getValue().getTargetId();
                        // just substitute the substitution target
                        e.setValue(compositionCouple.child.getSubstitutionMapping().getRequirements().get(targetCapability));
                    }
                }
            }
        }
        // merge each child nodes into the parent
        compositionCouple.parent.getNodeTemplates().putAll(compositionCouple.child.getNodeTemplates());
    }

    /**
     * Ugly code : since we don't name outputs in alien topology, we are not able to determine if an output is related to a property, to an attribute or to a
     * capability property. This is done in the same order than alien4cloud.topology.TopologyServiceCore.updateSubstitutionType(Topology) processes substitution
     * outputs.
     */
    private void sustituteGetAttribute(Topology child, Topology parent, String proxyAttributeName) {
        if (child.getOutputAttributes() != null) {
            for (Entry<String, Set<String>> oae : child.getOutputAttributes().entrySet()) {
                String nodeName = oae.getKey();
                for (String oa : oae.getValue()) {
                    if (oa.equals(proxyAttributeName)) {
                        // ok the proxy attribute name matches the embedded node attribute
                        Map<String, Set<String>> parentOas = parent.getOutputAttributes();
                        if (parentOas == null) {
                            parentOas = Maps.newHashMap();
                            parent.setOutputAttributes(parentOas);
                        }
                        Set<String> parentNodeOas = parentOas.get(nodeName);
                        if (parentNodeOas == null) {
                            parentNodeOas = Sets.newHashSet();
                            parentOas.put(nodeName, parentNodeOas);
                        }
                        parentNodeOas.add(proxyAttributeName);
                        return;
                    }
                }
            }
        }
        if (child.getOutputProperties() != null) {
            for (Entry<String, Set<String>> ope : child.getOutputProperties().entrySet()) {
                String nodeName = ope.getKey();
                for (String op : ope.getValue()) {
                    if (op.equals(proxyAttributeName)) {
                        // ok the proxy attribute name matches the embedded node property
                        Map<String, Set<String>> parentOps = parent.getOutputProperties();
                        if (parentOps == null) {
                            parentOps = Maps.newHashMap();
                            parent.setOutputProperties(parentOps);
                        }
                        Set<String> parentNodeOps = parentOps.get(nodeName);
                        if (parentNodeOps == null) {
                            parentNodeOps = Sets.newHashSet();
                            parentOps.put(nodeName, parentNodeOps);
                        }
                        parentNodeOps.add(proxyAttributeName);
                        return;
                    }
                }
            }
        }
        if (child.getOutputCapabilityProperties() != null) {
            for (Entry<String, Map<String, Set<String>>> ocpe : child.getOutputCapabilityProperties().entrySet()) {
                String nodeName = ocpe.getKey();
                for (Entry<String, Set<String>> cpes : ocpe.getValue().entrySet()) {
                    String embededCapabilityName = cpes.getKey();
                    for (String op : cpes.getValue()) {
                        if (op.equals(proxyAttributeName)) {
                            // ok the embedded output capability property matches the proxy type output attribute
                            Map<String, Map<String, Set<String>>> parentOcps = parent.getOutputCapabilityProperties();
                            if (parentOcps == null) {
                                parentOcps = Maps.newHashMap();
                                parent.setOutputCapabilityProperties(parentOcps);
                            }
                            Map<String, Set<String>> parentNodeOcps = parentOcps.get(nodeName);
                            if (parentNodeOcps == null) {
                                parentNodeOcps = Maps.newHashMap();
                                parentOcps.put(nodeName, parentNodeOcps);
                            }
                            Set<String> parentCapabilityOps = parentNodeOcps.get(embededCapabilityName);
                            if (parentCapabilityOps == null) {
                                parentCapabilityOps = Sets.newHashSet();
                                parentNodeOcps.put(embededCapabilityName, parentCapabilityOps);
                            }
                            parentCapabilityOps.add(proxyAttributeName);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Deeply explore this topology to detect if some type must be substituted by the corresponding topology template content and feed the {@link Deque}. <br>
     * BTW, rename the nodes by prefixing all the node names.
     */
    private void recursivelyBuildSubstitutionStack(Topology topology, Deque<CompositionCouple> stack, String prefix) {
        if (topology == null || topology.getNodeTemplates() == null || topology.getNodeTemplates().isEmpty()) {
            return;
        }
        for (Entry<String, NodeTemplate> nodeEntry : topology.getNodeTemplates().entrySet()) {
            String nodeName = nodeEntry.getKey();
            String type = nodeEntry.getValue().getType();
            IndexedNodeType nodeType = csarRepoSearchService.getElementInDependencies(IndexedNodeType.class, type, topology.getDependencies());
            if (nodeType.getSubstitutionTopologyId() != null) {
                // this node type is a proxy for a topology template
                Topology child = topologyServiceCore.getOrFail(nodeType.getSubstitutionTopologyId());
                CompositionCouple couple = new CompositionCouple(topology, child, nodeName, nodeName + "_");
                renameNodes(couple);
                stack.offer(couple);
                recursivelyBuildSubstitutionStack(child, stack, nodeName + "_");
            }
        }
    }

    private void renameNodes(CompositionCouple compositionCouple) {
        Topology topology = compositionCouple.child;
        String[] nodeNames = new String[topology.getNodeTemplates().size()];
        nodeNames = topology.getNodeTemplates().keySet().toArray(nodeNames);
        for (String nodeName : nodeNames) {
            String newName = ensureNodeNameIsUnique(topology.getNodeTemplates().keySet(), compositionCouple.nodeNamePrefix + nodeName, 0);
            renameNodeTemplate(topology, nodeName, newName);
        }
    }

    private String ensureNodeNameIsUnique(Set<String> keys, String prefix, int suffixeNumber) {
        String name = (suffixeNumber > 0) ? prefix + suffixeNumber : prefix;
        if (keys.contains(name)) {
            return ensureNodeNameIsUnique(keys, prefix, ++suffixeNumber);
        } else {
            return name;
        }
    }

    private void renameNodeTemplate(Topology topology, String oldName, String newName) {
        // if the prefixed name is already used by another node ?
        // quite improbable but ...
        if (topology.getNodeTemplates().containsKey(newName)) {
            throw new AlreadyExistException(String.format("A node with name '%s' already exists in this topology", newName));
        }
        NodeTemplate nodeTemplate = topology.getNodeTemplates().remove(oldName);
        // manage relationships that target this node
        for (NodeTemplate otherNodes : topology.getNodeTemplates().values()) {
            if (otherNodes.getRelationships() == null || otherNodes.getRelationships().isEmpty()) {
                continue;
            }
            for (RelationshipTemplate relationshipTemplate : otherNodes.getRelationships().values()) {
                if (relationshipTemplate.getTarget().equals(oldName)) {
                    relationshipTemplate.setTarget(newName);
                }
            }
        }
        // all output stuffs
        MapUtil.replaceKey(topology.getOutputProperties(), oldName, newName);
        MapUtil.replaceKey(topology.getOutputCapabilityProperties(), oldName, newName);
        MapUtil.replaceKey(topology.getOutputAttributes(), oldName, newName);
        // group members must be updated
        if (topology.getGroups() != null) {
            for (NodeGroup nodeGroup : topology.getGroups().values()) {
                Set<String> members = nodeGroup.getMembers();
                if (members != null && members.remove(oldName)) {
                    members.add(newName);
                }
            }
        }
        // substitutions
        if (topology.getSubstitutionMapping() != null) {
            renameNodeTemplateInSubstitutionTargets(topology.getSubstitutionMapping().getCapabilities(), oldName, newName);
            renameNodeTemplateInSubstitutionTargets(topology.getSubstitutionMapping().getRequirements(), oldName, newName);
        }
        // finally the node itself
        topology.getNodeTemplates().put(newName, nodeTemplate);
    }

    private void renameNodeTemplateInSubstitutionTargets(Map<String, SubstitutionTarget> substitutionTargets, String oldName, String newName) {
        if (substitutionTargets != null) {
            for (SubstitutionTarget s : substitutionTargets.values()) {
                if (s.getNodeTemplateName().equals(oldName)) {
                    s.setNodeTemplateName(newName);
                }
            }
        }
    }

    /**
     * Deeply explore composition in order to detect cyclic reference: if a descendant references the mainTopologyId.
     */
    public void recursivelyDetectTopologyCompositionCyclicReference(String mainTopologyId, String substitutionTopologyId) {
        Topology child = topologyServiceCore.getOrFail(substitutionTopologyId);
        if (child == null || child.getNodeTemplates() == null || child.getNodeTemplates().isEmpty()) {
            return;
        }
        for (Entry<String, NodeTemplate> nodeEntry : child.getNodeTemplates().entrySet()) {
            String type = nodeEntry.getValue().getType();
            IndexedNodeType nodeType = csarRepoSearchService.getElementInDependencies(IndexedNodeType.class, type, child.getDependencies());
            if (nodeType.getSubstitutionTopologyId() != null) {
                if (nodeType.getSubstitutionTopologyId().equals(mainTopologyId)) {
                    throw new CyclicReferenceException("Cyclic reference : a topology template can not reference itself (even indirectly)");
                }
                recursivelyDetectTopologyCompositionCyclicReference(mainTopologyId, nodeType.getSubstitutionTopologyId());
            }
        }
    }

    private static class CompositionCouple {
        /** The topology that embeds another one. */
        private final Topology parent;

        /** The topology template that will substitute the type. */
        private final Topology child;

        /** The node name referencing the child in the parent. */
        private final String nodeName;

        /** The prefix that will be used to rename nodes. */
        private final String nodeNamePrefix;

        public CompositionCouple(Topology parent, Topology child, String nodeName, String nodeNamePrefix) {
            super();
            this.parent = parent;
            this.child = child;
            this.nodeName = nodeName;
            this.nodeNamePrefix = nodeNamePrefix;
        }
    }

}
