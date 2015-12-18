package alien4cloud.deployment;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import alien4cloud.common.AlienConstants;
import alien4cloud.deployment.matching.services.nodes.NodeMatcherService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.LocationPlacementPolicy;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.PropertyUtil;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Service
public class DeploymentNodeSubstitutionService {
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private NodeMatcherService nodeMatcherService;
    @Inject
    private LocationResourceService locationResourceService;

    /**
     * Get all available substitutions (LocationResourceTemplate) for the given node templates with given dependencies and location groups
     *
     * @param nodeTemplates the node template to check
     * @param dependencies dependencies of those node templates
     * @param locationGroups group of location policy
     * @return a map which contains mapping from node template id to its available substitutions
     */
    private Map<String, List<LocationResourceTemplate>> getAvailableSubstitutions(Map<String, NodeTemplate> nodeTemplates, Set<CSARDependency> dependencies,
            Map<String, NodeGroup> locationGroups) {
        Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromDependencies(nodeTemplates, dependencies, false, false);
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = Maps.newHashMap();
        for (final Map.Entry<String, NodeGroup> locationGroupEntry : locationGroups.entrySet()) {
            String groupName = locationGroupEntry.getKey();
            final NodeGroup locationNodeGroup = locationGroupEntry.getValue();
            Map<String, NodeTemplate> nodesToMatch = Maps.newHashMap();
            if (MapUtils.isNotEmpty(nodeTemplates)) {
                if (AlienConstants.GROUP_ALL.equals(groupName)) {
                    locationNodeGroup.setMembers(nodeTemplates.keySet());
                    nodesToMatch = nodeTemplates;
                } else {
                    nodesToMatch = Maps.filterEntries(nodeTemplates, new Predicate<Map.Entry<String, NodeTemplate>>() {
                        @Override
                        public boolean apply(Map.Entry<String, NodeTemplate> input) {
                            return locationNodeGroup.getMembers().contains(input.getKey());
                        }
                    });
                }
            }
            LocationPlacementPolicy locationPlacementPolicy = (LocationPlacementPolicy) locationGroupEntry.getValue().getPolicies().get(0);
            availableSubstitutions.putAll(nodeMatcherService.match(nodeTypes, nodesToMatch, locationPlacementPolicy.getLocationId()));
        }
        return availableSubstitutions;
    }

    /**
     * Get all available substitutions for a processed deployment topology
     *
     * @param deploymentTopology
     * @return
     */
    public Map<String, List<LocationResourceTemplate>> getAvailableSubstitutions(DeploymentTopology deploymentTopology) {
        return getAvailableSubstitutions(deploymentTopology.getOriginalNodes(), deploymentTopology.getDependencies(), deploymentTopology.getLocationGroups());
    }

    /**
     * Process node substitution for the deployment topology
     *
     * @param deploymentTopology the deployment topology to process substitution
     */

    /**
     * This method updates the node substitution choices and default selections for a given deployment topology.
     *
     * @param deploymentTopology The deployment topology in which to save substitutions / deploymentTopology.getNodeTemplates() are the nodes from the original
     *            topology.
     * @param nodesToMergeProperties The node that where substituted previously with specific configurations from deployment user.
     */
    public void processNodesSubstitution(DeploymentTopology deploymentTopology, Map<String, NodeTemplate> nodesToMergeProperties) {
        if (MapUtils.isEmpty(deploymentTopology.getLocationGroups())) {
            // No location group is defined do nothing
            return;
        }
        deploymentTopology.getDependencies().addAll(deploymentTopology.getLocationDependencies());
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = getAvailableSubstitutions(deploymentTopology.getNodeTemplates(),
                deploymentTopology.getDependencies(), deploymentTopology.getLocationGroups());
        Map<String, Set<String>> availableSubstitutionsIds = Maps.newHashMap();
        for (Map.Entry<String, List<LocationResourceTemplate>> availableSubstitutionEntry : availableSubstitutions.entrySet()) {
            Set<String> ids = Sets.newHashSet();
            for (LocationResourceTemplate availableSubstitution : availableSubstitutionEntry.getValue()) {
                ids.add(availableSubstitution.getId());
            }
            availableSubstitutionsIds.put(availableSubstitutionEntry.getKey(), ids);
        }
        Map<String, String> substitutedNodes = deploymentTopology.getSubstitutedNodes();

        removeUnsynchronizedSubstitutions(deploymentTopology, substitutedNodes, availableSubstitutions);

        // clean the originalNodes map since some nodes might have been deleted from the initial topology, and thus not appearing in the availableSubstitutions
        // also update the original node, since it might have changed since it was added
        Iterator<Entry<String, NodeTemplate>> originalNodesIter = deploymentTopology.getOriginalNodes().entrySet().iterator();
        while (originalNodesIter.hasNext()) {
            Entry<String, NodeTemplate> next = originalNodesIter.next();
            if (!availableSubstitutions.containsKey(next.getKey())) {
                originalNodesIter.remove();
            } else { // override with the latest value.
                next.setValue(deploymentTopology.getNodeTemplates().get(next.getKey()));
            }
        }

        for (Map.Entry<String, List<LocationResourceTemplate>> entry : availableSubstitutions.entrySet()) {
            // select default values
            if (!substitutedNodes.containsKey(entry.getKey())) {
                deploymentTopology.getOriginalNodes().put(entry.getKey(), deploymentTopology.getNodeTemplates().get(entry.getKey()));
                if (!entry.getValue().isEmpty()) {
                    // Only take the first element as selected if no configuration has been set before
                    substitutedNodes.put(entry.getKey(), entry.getValue().iterator().next().getId());
                }
            }
        }

        deploymentTopology.setSubstitutedNodes(substitutedNodes);
        for (Map.Entry<String, String> substitutedNodeEntry : substitutedNodes.entrySet()) {
            // Substitute the node template of the topology by those matched
            NodeTemplate locationNode = locationResourceService.getOrFail(substitutedNodeEntry.getValue()).getTemplate();
            NodeTemplate abstractTopologyNode = deploymentTopology.getNodeTemplates().put(substitutedNodeEntry.getKey(), locationNode);
            NodeTemplate previousNode = null;
            if (nodesToMergeProperties != null) {
                previousNode = nodesToMergeProperties.get(substitutedNodeEntry.getKey());
            }
            // TODO define what need to be merged and what not to be merged
            // Merge name, properties and capability properties
            locationNode.setName(abstractTopologyNode.getName());
            // Also merge relationships
            locationNode.setRelationships(abstractTopologyNode.getRelationships());
            if (MapUtils.isNotEmpty(locationNode.getProperties())) {
                // Merge properties from previous values
                Set<String> keysToConsider = locationNode.getProperties().keySet();
                Map<String, AbstractPropertyValue> mergedProperties = Maps.newLinkedHashMap();
                if (previousNode != null && MapUtils.isNotEmpty(previousNode.getProperties())) {
                    PropertyUtil.mergeProperties(previousNode.getProperties(), mergedProperties, keysToConsider);
                }
                // Merge properties from abstract topology
                if (MapUtils.isNotEmpty(abstractTopologyNode.getProperties())) {
                    PropertyUtil.mergeProperties(abstractTopologyNode.getProperties(), mergedProperties, keysToConsider);
                }
                // Merge properties from location resources
                if (MapUtils.isNotEmpty(locationNode.getProperties())) {
                    PropertyUtil.mergeProperties(locationNode.getProperties(), mergedProperties, keysToConsider);
                }
                locationNode.setProperties(mergedProperties);
            }
            if (MapUtils.isNotEmpty(locationNode.getCapabilities())) {
                // The location node is the node from orchestrator which must be a child type of the abstract topology node so should loop on this node to do
                // not miss any capability
                for (Map.Entry<String, Capability> locationCapabilityEntry : locationNode.getCapabilities().entrySet()) {
                    Capability locationCapability = locationCapabilityEntry.getValue();
                    if (MapUtils.isEmpty(locationCapability.getProperties())) {
                        continue;
                    }
                    Set<String> keysToConsider = locationCapability.getProperties().keySet();
                    Map<String, AbstractPropertyValue> mergedCapabilityProperties = Maps.newLinkedHashMap();

                    // Merge from previous existing nodes
                    Capability previousCapability = null;
                    if (previousNode != null && MapUtils.isNotEmpty(previousNode.getCapabilities())) {
                        previousCapability = previousNode.getCapabilities().get(locationCapabilityEntry.getKey());
                    }
                    if (previousCapability != null && MapUtils.isNotEmpty(previousCapability.getProperties())) {
                        PropertyUtil.mergeProperties(previousCapability.getProperties(), mergedCapabilityProperties, keysToConsider);
                    }
                    // Merge from abstract topology
                    Capability abstractCapability = null;
                    if (MapUtils.isNotEmpty(abstractTopologyNode.getCapabilities())) {
                        abstractCapability = abstractTopologyNode.getCapabilities().get(locationCapabilityEntry.getKey());
                    }
                    if (abstractCapability != null && MapUtils.isNotEmpty(abstractCapability.getProperties())) {
                        PropertyUtil.mergeProperties(abstractCapability.getProperties(), mergedCapabilityProperties, keysToConsider);
                    }

                    // Merge from location resources
                    PropertyUtil.mergeProperties(locationCapability.getProperties(), mergedCapabilityProperties, keysToConsider);
                    locationCapability.setProperties(mergedCapabilityProperties);
                }
            }
        }
    }

    /**
     * This methods checks all the previously configured substitutions and ensures that they are still related to existing node and matching in the topology.
     *
     * @param deploymentTopology The deployment topology.
     * @param substitutedNodes The previous configuration for substitution nodes.
     * @param availableSubstitutions The substitutions provided by the location's node matching.
     */
    private void removeUnsynchronizedSubstitutions(DeploymentTopology deploymentTopology, Map<String, String> substitutedNodes,
            Map<String, List<LocationResourceTemplate>> availableSubstitutions) {
        if (deploymentTopology.getNodeTemplates() == null) {
            substitutedNodes.clear();
            return;
        }

        // When the user has removed some mapped nodes from the topology the previous substitution configuration still exits.
        Iterator<Map.Entry<String, String>> mappingEntryIterator = substitutedNodes.entrySet().iterator();
        while (mappingEntryIterator.hasNext()) {
            Map.Entry<String, String> entry = mappingEntryIterator.next();
            if (deploymentTopology.getNodeTemplates().containsKey(entry.getKey())) {
                // The node is still in the topology but we have to check that the existing substitution value is still a valid option.
                List<LocationResourceTemplate> availableSubstitutionsForNode = availableSubstitutions.get(entry.getKey());
                if (availableSubstitutionsForNode == null) {
                    // no options => remove existing mapping
                    mappingEntryIterator.remove();
                } else {
                    boolean substitutedTemplateExist = false;
                    for (LocationResourceTemplate availableSubstitutionForNode : availableSubstitutionsForNode) {
                        if (availableSubstitutionForNode.getId().equals(entry.getValue())) {
                            substitutedTemplateExist = true;
                            break;
                        }
                    }
                    if (!substitutedTemplateExist) {
                        // The mapping do not exist anymore in the match result
                        mappingEntryIterator.remove();
                    }
                }
            } else {
                // if node is not anymore in the topology just remove the entry
                mappingEntryIterator.remove();
            }
        }
    }
}
