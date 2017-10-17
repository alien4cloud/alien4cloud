package alien4cloud.deployment.matching.services.nodes;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alien4cloud.tosca.model.definitions.constraints.IMatchPropertyConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.deployment.matching.MatchingFilterDefinition;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of INodeMatcherPlugin to be used when no matching plugin has been defined.
 */
@Slf4j
@Component
public class DefaultNodeMatcher extends AbstractTemplateMatcher<LocationResourceTemplate, NodeTemplate, NodeType> implements INodeMatcherPlugin {
    /**
     * Match a node against a location.
     *
     * @param nodeTemplate The node template to match.
     * @param nodeType The node type that defines the type of the node template to match.
     * @param locationResources The resources configured for the location against which we are matching the nodes.
     */
    public List<LocationResourceTemplate> matchNode(NodeTemplate nodeTemplate, NodeType nodeType, LocationResources locationResources,
            Map<String, MatchingConfiguration> matchingConfigurations) {
        return super.match(nodeTemplate, nodeType, locationResources.getNodeTemplates(), locationResources.getNodeTypes(), locationResources,
                matchingConfigurations);
    }

    @Override
    protected boolean typeSpecificMatching(NodeTemplate abstractTemplate, LocationResourceTemplate candidate, NodeType candidateType,
            LocationResources locationResources, MatchingConfiguration matchingConfiguration) {

        for (Entry<String, Capability> candidateCapability : safe(candidate.getTemplate().getCapabilities()).entrySet()) {
            MatchingFilterDefinition configuredFilterDefinition = matchingConfiguration == null ? null
                    : matchingConfiguration.getCapabilities().get(candidateCapability.getKey());
            Map<String, List<IMatchPropertyConstraint>> configuredFilters = configuredFilterDefinition == null ? null
                    : configuredFilterDefinition.getProperties();
            CapabilityType capabilityType = locationResources.getCapabilityTypes().get(candidateCapability.getValue().getType());

            Capability templateCapability = safe(abstractTemplate.getCapabilities()).get(candidateCapability.getKey());

            if (templateCapability != null && !isValidTemplatePropertiesMatch(templateCapability.getProperties(),
                    candidateCapability.getValue().getProperties(), capabilityType.getProperties(), configuredFilters)) {
                return false;
            }
        }

        return true;
    }
}