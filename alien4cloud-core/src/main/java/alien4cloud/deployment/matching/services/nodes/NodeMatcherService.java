package alien4cloud.deployment.matching.services.nodes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.exception.InvalidArgumentException;
import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationMatchingConfigurationService;
import alien4cloud.orchestrators.locations.services.LocationService;

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
    @Lazy(true)
    private ILocationResourceService locationResourceService;
    @Inject
    private LocationMatchingConfigurationService locationMatchingConfigurationService;

    private INodeMatcherPlugin getNodeMatcherPlugin() {
        // TODO manage plugins
        return defaultNodeMatcher;
    }

    public Map<String, List<LocationResourceTemplate>> match(Map<String, NodeType> nodesTypes, Map<String, NodeTemplate> nodesToMatch,
                                                             String locationId) {
        Map<String, List<LocationResourceTemplate>> matchingResult = Maps.newHashMap();
        Location location = locationService.getOrFail(locationId);
        LocationResources locationResources = locationResourceService.getLocationResources(location);
        Map<String, MatchingConfiguration> matchingConfigurations = locationMatchingConfigurationService.getMatchingConfiguration(location);
        Set<String> typesManagedByLocation = Sets.newHashSet();
        for (NodeType nodeType : locationResources.getNodeTypes().values()) {
            typesManagedByLocation.add(nodeType.getElementId());
            typesManagedByLocation.addAll(nodeType.getDerivedFrom());
        }
        INodeMatcherPlugin nodeMatcherPlugin = getNodeMatcherPlugin();
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : nodesToMatch.entrySet()) {
            String nodeTemplateId = nodeTemplateEntry.getKey();
            NodeTemplate nodeTemplate = nodeTemplateEntry.getValue();
            if (typesManagedByLocation.contains(nodeTemplate.getType())) {
                NodeType nodeTemplateType = nodesTypes.get(nodeTemplate.getType());
                if (nodeTemplateType == null) {
                    throw new InvalidArgumentException("The given node types map must contain the type of the node template");
                }
                matchingResult.put(nodeTemplateId, nodeMatcherPlugin.matchNode(nodeTemplate, nodeTemplateType, locationResources, matchingConfigurations));
            }
        }
        return matchingResult;
    }
}
