package alien4cloud.deployment.matching.plugins;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import org.alien4cloud.tosca.model.templates.NodeTemplate;

/**
 * This plugin is used to match topology nodes against resources provided by locations.
 */
public interface INodeMatcherPlugin {
    List<LocationResourceTemplate> matchNode(NodeTemplate nodeTemplate, NodeType nodeType, LocationResources locationResources,
                                             Map<String, MatchingConfiguration> matchingConfigurations);
}