package alien4cloud.deployment;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import alien4cloud.common.AlienConstants;
import alien4cloud.deployment.matching.services.nodes.NodeMatcherService;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.topology.LocationPlacementPolicy;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.topology.TopologyServiceCore;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

@Service
public class DeploymentNodeSubstitutionService {

    @Inject
    private TopologyServiceCore topologyServiceCore;

    @Inject
    private NodeMatcherService nodeMatcherService;

    /**
     * Get all available substitutions (LocationResourceTemplate) for the given deployment topology
     * 
     * @param deploymentTopology the deployment topology
     * @return a map which contains mapping from node template id to its available substitutions
     */
    public Map<String, List<LocationResourceTemplate>> getAvailableSubstitutions(DeploymentTopology deploymentTopology) {
        Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(deploymentTopology, false, false);
        Map<String, NodeGroup> locationGroups = deploymentTopology.getLocationGroups();
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = Maps.newHashMap();
        for (final Map.Entry<String, NodeGroup> locationGroupEntry : locationGroups.entrySet()) {
            String groupName = locationGroupEntry.getKey();
            final NodeGroup locationNodeGroup = locationGroupEntry.getValue();
            Map<String, NodeTemplate> nodesToMatch;
            if (AlienConstants.GROUP_ALL.equals(groupName)) {
                locationNodeGroup.setMembers(deploymentTopology.getNodeTemplates().keySet());
                nodesToMatch = deploymentTopology.getNodeTemplates();
            } else {
                nodesToMatch = Maps.filterEntries(deploymentTopology.getNodeTemplates(), new Predicate<Map.Entry<String, NodeTemplate>>() {
                    @Override
                    public boolean apply(Map.Entry<String, NodeTemplate> input) {
                        return locationNodeGroup.getMembers().contains(input.getKey());
                    }
                });
            }
            LocationPlacementPolicy locationPlacementPolicy = (LocationPlacementPolicy) locationGroupEntry.getValue().getPolicies().get(0);
            availableSubstitutions.putAll(nodeMatcherService.match(nodeTypes, nodesToMatch, locationPlacementPolicy.getLocationId()));
        }
        return availableSubstitutions;
    }

    /**
     * Process node substitution for the deployment topology and returns true if it has been changed
     * 
     * @param deploymentTopology the deployment topology to process substitution
     * @return true if the deployment has been changed
     */
    public boolean processNodesSubstitution(DeploymentTopology deploymentTopology) {
        if (MapUtils.isEmpty(deploymentTopology.getLocationGroups())) {
            // No location group is defined do nothing
            return false;
        }
        deploymentTopology.getDependencies().addAll(deploymentTopology.getLocationDependencies());
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = getAvailableSubstitutions(deploymentTopology);
        Map<String, LocationResourceTemplate> substitutedNodes = deploymentTopology.getSubstitutedNodes();
        boolean changed = false;
        if (MapUtils.isEmpty(substitutedNodes)) {
            changed = true;
            substitutedNodes = Maps.newHashMap();
        } else {
            // Try to remove unknown mapping from existing config
            Iterator<Map.Entry<String, LocationResourceTemplate>> mappingEntryIterator = substitutedNodes.entrySet().iterator();
            while (mappingEntryIterator.hasNext()) {
                Map.Entry<String, LocationResourceTemplate> entry = mappingEntryIterator.next();
                if (deploymentTopology.getNodeTemplates() == null || !deploymentTopology.getNodeTemplates().containsKey(entry.getKey())) {
                    if (!availableSubstitutions.containsKey(entry.getKey())) {
                        // Remove the mapping if topology do not contain the node with that name and of type compute
                        changed = true;
                        mappingEntryIterator.remove();
                    } else {
                        List<LocationResourceTemplate> availableSubstitutionsForNode = availableSubstitutions.get(entry.getKey());
                        boolean substitutedTemplateExist = false;
                        for (LocationResourceTemplate availableSubstitutionForNode : availableSubstitutionsForNode) {
                            if (availableSubstitutionForNode.getId().equals(entry.getValue().getId())) {
                                substitutedTemplateExist = true;
                                break;
                            }
                        }
                        if (!substitutedTemplateExist) {
                            // The mapping do not exist anymore in the match result
                            changed = true;
                            mappingEntryIterator.remove();
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, List<LocationResourceTemplate>> entry : availableSubstitutions.entrySet()) {
            if (entry.getValue().isEmpty()) {
                // Not substituted
                substitutedNodes.put(entry.getKey(), null);
            } else if (!substitutedNodes.containsKey(entry.getKey())) {
                // Only take the first element as selected if no configuration has been set before
                changed = true;
                substitutedNodes.put(entry.getKey(), entry.getValue().iterator().next());
            }
        }
        if (changed) {
            deploymentTopology.setSubstitutedNodes(substitutedNodes);
            for (Map.Entry<String, LocationResourceTemplate> substitutedNodeEntry : substitutedNodes.entrySet()) {
                // Substitute the node template of the topology by those matched
                deploymentTopology.getNodeTemplates().put(substitutedNodeEntry.getKey(), substitutedNodeEntry.getValue().getTemplate());
            }
        }
        return changed;
    }
}
