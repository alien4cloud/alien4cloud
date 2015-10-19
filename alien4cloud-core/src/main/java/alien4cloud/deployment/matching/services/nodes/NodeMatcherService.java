package alien4cloud.deployment.matching.services.nodes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationMatchingConfigurationService;
import alien4cloud.orchestrators.locations.services.LocationService;
import org.springframework.stereotype.Service;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceService;

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
    private LocationService locationService;
    @Inject
    private LocationResourceService locationResourceService;
    @Inject
    private LocationMatchingConfigurationService locationMatchingConfigurationService;

    private INodeMatcherPlugin getNodeMatcherPlugin() {
        // TODO manage plugins
        return defaultNodeMatcher;
    }

    public Map<String, List<LocationResourceTemplate>> match(Map<String, IndexedNodeType> nodesTypes, Map<String, NodeTemplate> nodesToMatch,
            String locationId) {
        Map<String, List<LocationResourceTemplate>> matchingResult = Maps.newHashMap();
        Location location = locationService.getOrFail(locationId);
        LocationResources locationResources = locationResourceService.getLocationResources(location);
        Map<String, MatchingConfiguration> matchingConfigurations = locationMatchingConfigurationService.getMatchingConfiguration(location);
        Set<String> typesManagedByLocation = Sets.newHashSet();
        for (IndexedNodeType nodeType : locationResources.getNodeTypes().values()) {
            typesManagedByLocation.add(nodeType.getElementId());
            typesManagedByLocation.addAll(nodeType.getDerivedFrom());
        }
        INodeMatcherPlugin nodeMatcherPlugin = getNodeMatcherPlugin();
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : nodesToMatch.entrySet()) {
            String nodeTemplateId = nodeTemplateEntry.getKey();
            NodeTemplate nodeTemplate = nodeTemplateEntry.getValue();
            if (typesManagedByLocation.contains(nodeTemplate.getType())) {
                IndexedNodeType nodeTemplateType = nodesTypes.get(nodeTemplate.getType());
                if (nodeTemplateType == null) {
                    throw new InvalidArgumentException("The given node types map must contain the type of the node template");
                }
                matchingResult.put(nodeTemplateId, nodeMatcherPlugin.matchNode(nodeTemplate, nodeTemplateType, locationResources, matchingConfigurations));
            }
        }
        return matchingResult;
    }
}
