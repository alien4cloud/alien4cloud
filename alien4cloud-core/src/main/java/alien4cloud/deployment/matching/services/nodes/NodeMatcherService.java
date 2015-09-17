package alien4cloud.deployment.matching.services.nodes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceService;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyService;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Node matcher service will filter location resources for all substitutable nodes of the topology. It will return only location resources that can substitute a
 * node.
 */
@Service
public class NodeMatcherService {

    @Inject
    private DefaultNodeMatcher defaultNodeMatcher;

    @Inject
    private TopologyService topologyService;

    @Inject
    private LocationResourceService locationResourceService;

    private INodeMatcherPlugin getNodeMatcherPlugin() {
        // TODO manage plugins
        return defaultNodeMatcher;
    }

    public Map<String, List<LocationResourceTemplate>> match(String topologyId, String locationId) {
        TopologyDTO topologyDTO = topologyService.getTopologyDTO(topologyId);
        Map<String, List<LocationResourceTemplate>> matchingResult = Maps.newHashMap();
        LocationResources locationResources = locationResourceService.getLocationResources(locationId);
        Set<String> typesManagedByLocation = Sets.newHashSet();
        for (IndexedNodeType nodeType : locationResources.getNodeTypes().values()) {
            typesManagedByLocation.add(nodeType.getElementId());
            typesManagedByLocation.addAll(nodeType.getDerivedFrom());
        }
        INodeMatcherPlugin nodeMatcherPlugin = getNodeMatcherPlugin();
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : topologyDTO.getTopology().getNodeTemplates().entrySet()) {
            String nodeTemplateId = nodeTemplateEntry.getKey();
            NodeTemplate nodeTemplate = nodeTemplateEntry.getValue();
            if (typesManagedByLocation.contains(nodeTemplate.getType())) {
                IndexedNodeType nodeTemplateType = topologyDTO.getNodeTypes().get(nodeTemplate.getType());
                matchingResult.put(nodeTemplateId, nodeMatcherPlugin.matchNode(nodeTemplate, nodeTemplateType, locationResources));
            }
        }
        return matchingResult;
    }
}
