package alien4cloud.deployment.matching.services.nodes;

import java.util.List;

import org.springframework.stereotype.Component;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.model.topology.NodeTemplate;

import com.google.common.collect.Lists;

/**
 * Default implementation of INodeMatcherPlugin to be used when no matching plugin has been defined.
 */
@Component
public class DefaultNodeMatcher implements INodeMatcherPlugin {

    // @Inject
    // private MatchingConfigurationService matchingConfigurationService;

    // TODO initialize default matching configuration based on parsing a yaml file within a4c

    /**
     * Match a node against a location.
     *
     * @param nodeTemplate The node template to match.
     * @param nodeType The node type that defines the type of the node template to match.
     * @param locationResources The resources configured for the location against which we are matching the nodes.
     */
    public List<LocationResourceTemplate> matchNode(NodeTemplate nodeTemplate, IndexedNodeType nodeType, LocationResources locationResources) {
        // TODO check if the node template candidate has any specified operation or relation operations
        // if so reject service matching for this node as it is not possible to execute operations on services

        // perform nodes resources matching
        // TODO perform service matching
        /*
         * TODO Refine node matching by considering specific matching rules for the node. If no constraint is specified in a matching configuration then equals
         * constraint is applied.
         * matchingConfigurationService.getMatchingConfiguration(nodeType);
         */
        List<LocationResourceTemplate> candidates = locationResources.getNodeTemplates();
        List<LocationResourceTemplate> matchingResults = Lists.newArrayList();
        for (LocationResourceTemplate candidate : candidates) {
            String candidateTypeName = candidate.getTemplate().getType();
            IndexedNodeType candidateType = locationResources.getNodeTypes().get(candidateTypeName);
            // For the moment only match by node type
            if (candidateType.getDerivedFrom() != null && candidateType.getDerivedFrom().contains(nodeTemplate.getType())) {
                matchingResults.add(candidate);
            }
        }
        return matchingResults;
    }

}