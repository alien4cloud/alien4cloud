package alien4cloud.deployment.matching.plugins;

import java.util.List;
import java.util.Map;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.model.topology.NodeTemplate;

/**
 * This plugin is used to match topology nodes against resources provided by locations.
 */
public interface INodeMatcherPlugin {
    List<LocationResourceTemplate> matchNode(NodeTemplate nodeTemplate, IndexedNodeType nodeType, LocationResources locationResources,
            Map<String, MatchingConfiguration> matchingConfigurations);
}