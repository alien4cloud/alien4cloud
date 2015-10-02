package alien4cloud.deployment.matching.services.nodes;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.model.components.*;
import alien4cloud.model.components.constraints.IMatchPropertyConstraint;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.deployment.matching.MatchingFilterDefinition;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.topology.validation.NodeFilterValidationService;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.google.common.collect.Lists;

/**
 * Default implementation of INodeMatcherPlugin to be used when no matching plugin has been defined.
 */
@Slf4j
@Component
public class DefaultNodeMatcher implements INodeMatcherPlugin {
    @Inject
    private NodeFilterValidationService nodeFilterValidationService;
    // TODO initialize default matching configuration based on parsing a yaml file within a4c for nodes like Compute etc.

    /**
     * Match a node against a location.
     *
     * @param nodeTemplate The node template to match.
     * @param nodeType The node type that defines the type of the node template to match.
     * @param locationResources The resources configured for the location against which we are matching the nodes.
     */
    public List<LocationResourceTemplate> matchNode(NodeTemplate nodeTemplate, IndexedNodeType nodeType, LocationResources locationResources,
            Map<String, MatchingConfiguration> matchingConfigurations) {
        List<LocationResourceTemplate> matchingResults = Lists.newArrayList();

        List<LocationResourceTemplate> matchedServices = matchServices(nodeTemplate, nodeType, locationResources);
        matchingResults.addAll(matchedServices);

        List<LocationResourceTemplate> matchedOnDemands = matchedOnDemands(nodeTemplate, nodeType, locationResources, matchingConfigurations);
        matchingResults.addAll(matchedOnDemands);

        return matchingResults;
    }

    /**
     * Match a node against the services provided by a location.
     * 
     * @param nodeTemplate The node template to match.
     * @param nodeType The node type that defines the type of the node template to match.
     * @param locationResources The resources configured for the location against which we are matching the nodes.
     */
    private List<LocationResourceTemplate> matchServices(NodeTemplate nodeTemplate, IndexedNodeType nodeType, LocationResources locationResources) {
        // TODO perform service matching

        // check if the node template candidate has any specified operation or relation operations if so reject service matching for this node as it is not
        // possible to execute operations on services

        return Lists.newArrayList();
    }

    /**
     * Match a node against the on demand resources provided by a location.
     *
     * @param nodeTemplate The node template to match.
     * @param nodeType The node type that defines the type of the node template to match.
     * @param locationResources The resources configured for the location against which we are matching the nodes.
     */
    private List<LocationResourceTemplate> matchedOnDemands(NodeTemplate nodeTemplate, IndexedNodeType nodeType, LocationResources locationResources,
            Map<String, MatchingConfiguration> matchingConfigurations) {
        /*
         * TODO Refine node matching by considering specific matching rules for the node. If no constraint is specified in a matching configuration then equals
         * constraint is applied.
         */
        List<LocationResourceTemplate> matchingResults = Lists.newArrayList();
        List<LocationResourceTemplate> candidates = locationResources.getNodeTemplates();
        for (LocationResourceTemplate candidate : candidates) {
            String candidateTypeName = candidate.getTemplate().getType();
            IndexedNodeType candidateType = locationResources.getNodeTypes().get(candidateTypeName);
            // For the moment only match by node type
            if (isValidCandidate(nodeTemplate, nodeType, candidate, candidateType, locationResources.getCapabilityTypes(), matchingConfigurations)) {
                matchingResults.add(candidate);
            }
        }

        // TODO Sort the matching results to get the best match for the driver.
        return matchingResults;
    }

    /**
     * Checks if the type of a LocationResourceTemplate is matching the expected type.
     *
     * @param nodeTemplate The node template to match.
     * @param nodeType The type of the node template to match.
     * @param candidateType The type of the candidate node.
     * @param candidate The candidate location resource.
     * @param capabilityTypes Map of capability types that may be used by the candidateType.
     * @return True if the candidate is a valid match for the node template.
     */
    private boolean isValidCandidate(NodeTemplate nodeTemplate, IndexedNodeType nodeType, LocationResourceTemplate candidate, IndexedNodeType candidateType,
            Map<String, IndexedCapabilityType> capabilityTypes, Map<String, MatchingConfiguration> matchingConfigurations) {
        // Check that the candidate node type is valid
        if (!isCandidateTypeValid(nodeTemplate, candidate, candidateType)) {
            return false;
        }

        // Check that the note template properties are matching the constraints specified for matching.
        MatchingConfiguration matchingConfiguration = matchingConfigurations.get(nodeType.getElementId());

        if (matchingConfiguration == null) {
            return true;
        }

        // create a node filter based on all properties configured on the candidate node
        return isTemplatePropertiesMatchCandidateFilters(nodeTemplate, matchingConfiguration, candidate, candidateType, capabilityTypes);
    }

    private boolean isTemplatePropertiesMatchCandidateFilters(NodeTemplate nodeTemplate, MatchingConfiguration matchingConfiguration,
            LocationResourceTemplate candidate, IndexedNodeType candidateType, Map<String, IndexedCapabilityType> capabilityTypes) {
        // check that the node root properties matches the filters defined on the MatchingConfigurations.
        if (!isTemplatePropertiesMatchCandidateFilter(nodeTemplate.getProperties(), matchingConfiguration.getProperties(),
                candidate.getTemplate().getProperties(), candidateType.getProperties())) {
            return false;
        }

        // check that the properties defined on the capabilities matches the filters configured for the capabilities
        for (Map.Entry<String, MatchingFilterDefinition> capabilityMatchingFilterEntry : matchingConfiguration.getCapabilities().entrySet()) {
            FilterDefinition filterDefinition = new FilterDefinition();

            Capability candidateCapability = candidate.getTemplate().getCapabilities().get(capabilityMatchingFilterEntry.getKey());
            IndexedCapabilityType capabilityType = capabilityTypes.get(candidateCapability.getType());
            Capability templateCapability = nodeTemplate.getCapabilities().get(capabilityMatchingFilterEntry.getKey());

            if (!isTemplatePropertiesMatchCandidateFilter(templateCapability.getProperties(), capabilityMatchingFilterEntry.getValue().getProperties(),
                    candidateCapability.getProperties(), capabilityType.getProperties())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Add filters from the matching configuration to the node filter that will be applied for matching only if a value is specified on the configuration
     * template.
     * 
     * @param nodeTemplateValues The properties values from the node template to match.
     * @param sourceFilters The filtering map (based on constraints) from matching configuration.
     * @param propertyValues The values defined on the Location Template.
     * @param propertyDefinitions The properties definitions associated with the node.
     */
    private boolean isTemplatePropertiesMatchCandidateFilter(Map<String, AbstractPropertyValue> nodeTemplateValues,
            Map<String, List<IMatchPropertyConstraint>> sourceFilters, Map<String, AbstractPropertyValue> propertyValues,
            Map<String, PropertyDefinition> propertyDefinitions) {
        for (Map.Entry<String, List<IMatchPropertyConstraint>> filterEntry : sourceFilters.entrySet()) {
            AbstractPropertyValue candidatePropertyValue = propertyValues.get(filterEntry.getKey());
            AbstractPropertyValue templatePropertyValue = nodeTemplateValues.get(filterEntry.getKey());
            if (candidatePropertyValue != null && candidatePropertyValue instanceof ScalarPropertyValue && templatePropertyValue != null
                    && templatePropertyValue instanceof ScalarPropertyValue) {
                try {
                    IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinitions.get(filterEntry.getKey()).getType());
                    // set the constraint value and add it to the node filter
                    for (IMatchPropertyConstraint constraint : filterEntry.getValue()) {
                        constraint.setConstraintValue(toscaType, ((ScalarPropertyValue) candidatePropertyValue).getValue());
                        try {
                            constraint.validate(toscaType, ((ScalarPropertyValue) templatePropertyValue).getValue());
                        } catch (ConstraintViolationException e) {
                            return false;
                        }
                    }
                } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                    log.debug("The value of property for a constraint is not valid.", e);
                }
            }
        }
        return true;
    }

    /**
     * Checks if the type of a LocationResourceTemplate is matching the expected type.
     * 
     * @param nodeTemplate The node template to match.
     * @param candidateType The type of the candidate node.
     * @param candidate The candidate location resource.
     * @return True if the candidate type matches the node template type, false if not.
     */
    private boolean isCandidateTypeValid(NodeTemplate nodeTemplate, LocationResourceTemplate candidate, IndexedNodeType candidateType) {
        return candidateType.getElementId().equals(nodeTemplate.getType())
                || (candidateType.getDerivedFrom() != null && candidateType.getDerivedFrom().contains(nodeTemplate.getType()));
    }
}