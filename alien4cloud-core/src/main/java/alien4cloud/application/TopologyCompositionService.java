package alien4cloud.application;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.SubstitutionTarget;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Maps;

@Slf4j
@Service
public class TopologyCompositionService {

    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;
    
    @Resource
    private TopologyServiceCore topologyServiceCore;

    public void processTopologyComposition(Topology topology) {
        Deque<CompositionCouple> stack = new ArrayDeque<CompositionCouple>();
        recursivelyBuildSubstitutionStack(topology, stack, "");
        // now this stack contains all the embeded topology templates
        if (!stack.isEmpty()) {
            // iterate over the stack in descending order (manage the deepest topologies at a first time).
            Iterator<CompositionCouple> compositionIterator = stack.descendingIterator();
            while (compositionIterator.hasNext()) {
                processComposition(compositionIterator.next());
            }
            // TODO: finally compile the direct child of the main topology
            // link between embeded child ?
        }
        log.debug("");
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
                if (pValue instanceof FunctionPropertyValue && ((FunctionPropertyValue)pValue).getFunction().equals(ToscaFunctionConstants.GET_INPUT)) {
                    String inputName = ((FunctionPropertyValue)pValue).getTemplateName();
                    propertyEntry.setValue(proxyNodeTemplate.getProperties().get(inputName));
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
     * Deeply explore this topology to detect if some type must be substituted by the corresponding topology template content and feed the {@link Deque}. <br>
     * BTW, rename the nodes by prefixing all the node names.
     */
    private void recursivelyBuildSubstitutionStack(Topology topology, Deque<CompositionCouple> stack, String prefix) {
        for (Entry<String, NodeTemplate> nodeEntry : topology.getNodeTemplates().entrySet()) {
            String nodeName = nodeEntry.getKey();
            String type = nodeEntry.getValue().getType();
            IndexedNodeType nodeType = csarRepoSearchService.getElementInDependencies(IndexedNodeType.class, type, topology.getDependencies());
            if (nodeType.getSubstitutionTopologyId() != null) {
                // this node type is a proxy for a topology template
                Topology child = topologyServiceCore.getMandatoryTopology(nodeType.getSubstitutionTopologyId());
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
