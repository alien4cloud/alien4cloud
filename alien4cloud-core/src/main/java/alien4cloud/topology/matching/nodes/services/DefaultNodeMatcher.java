package alien4cloud.topology.matching.nodes.services;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.orchestrators.rest.model.LocationResources;
import alien4cloud.topology.matching.nodes.model.MatchingConfiguration;
import alien4cloud.topology.matching.nodes.plugin.INodeMatcherPlugin;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.inject.Inject;

/**
 * Default implementation of INodeMatcherPlugin to be used when no matching plugin has been defined.
 */
@Component
public class DefaultNodeMatcher implements INodeMatcherPlugin {

    @Inject
    private MatchingConfigurationService matchingConfigurationService;

    // TODO initialize default matching configuration based on parsing a yaml file within a4c

    /**
     * Match a node against a location.
     *
     * @param nodeTemplate The node template to match.
     * @param nodeType The node type that defines the type of the node template to match.
     * @param locationResources The resources configured for the location against which we are matching the nodes.
     */
    public void matchNode(NodeTemplate nodeTemplate, IndexedNodeType nodeType, LocationResources locationResources) {
        // TODO check if the node template candidate has any specified operation or relation operations
        // if so reject service matching for this node as it is not possible to execute operations on services

        // perform nodes resources matching

        // perform service matching

        // If no constraint is specified in a matching configuration then equals constraint is applied.
        matchingConfigurationService.getMatchingConfiguration(nodeType);
    }


}