package alien4cloud.deployment.matching.services.nodes;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.IMatchPropertyConstraint;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.apache.commons.collections4.MapUtils;

import com.google.common.collect.Lists;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract implementation of matching for elements that are common to any abstract template.
 */
@Slf4j
public abstract class AbstractTemplateMatcher<R extends AbstractLocationResourceTemplate, V extends AbstractTemplate, T extends AbstractInheritableToscaType> {
    /**
     *
     * @param abstractTemplate
     * @param type
     * @param candidates
     * @param candidateTypes
     * @param matchingConfigurations
     */
    public List<R> match(V abstractTemplate, T type, List<R> candidates, Map<String, T> candidateTypes, LocationResources locationResources,
            Map<String, MatchingConfiguration> matchingConfigurations) {
        List<R> matchingResults = Lists.newArrayList();

        for (R candidate : candidates) {
            T candidateType = candidateTypes.get(candidate.getTemplate().getType());

            if (isValidCandidate(abstractTemplate, type, candidate, candidateType, locationResources, matchingConfigurations)) {
                matchingResults.add(candidate);
            }
        }

        // TODO Sort the matching results to get the best match for the driver.
        return matchingResults;
    }

    /**
     * Checks if the type of a LocationResourceTemplate is matching the expected type.
     *
     * @param abstractTemplate The template to match.
     * @param type the type of the template to match
     * @param candidate The candidate location resource.
     * @param candidateType The type of the candidate node.
     * @param locationResources The global location resource object that contains all location resources and types.
     * @return True if the candidate is a valid match for the node template.
     */
    private boolean isValidCandidate(V abstractTemplate, T type, R candidate, T candidateType, LocationResources locationResources,
            Map<String, MatchingConfiguration> matchingConfigurations) {
        // Check that the type of the candidate is valid.
        if (!isCandidateTypeValid(abstractTemplate, candidateType)) {
            return false;
        }

        if (candidate.isService()) {
            return true;
        }

        // Only abstract node type can be match against a service
        if (!type.isAbstract() && candidate.isService()) {
            return false;
        }

        MatchingConfiguration matchingConfiguration = getMatchingConfiguration(candidateType, safe(matchingConfigurations));
        // if (matchingConfiguration == null) {
        // return true;
        // }

        // create a node filter based on all properties configured on the candidate node
        return validateTemplateMatch(abstractTemplate, candidate, candidateType, locationResources, matchingConfiguration);
    }

    /**
     * Get the matching configuration for the substitution candidate based on its type hierarchy.
     *
     * Meaning if a candidateType D derives from (in this order) C, B, A, then we will first look for a matching for D. <br>
     * If not found, then look for the closest parent matching configuration, and so on until no more parent left.
     *
     * @param candidateType
     * @param matchingConfigurations
     * @return
     */
    private MatchingConfiguration getMatchingConfiguration(T candidateType, Map<String, MatchingConfiguration> matchingConfigurations) {
        MatchingConfiguration config = null;
        if (MapUtils.isNotEmpty(matchingConfigurations)) {
            List<String> typeHierarchy = Lists.newArrayList(candidateType.getElementId());
            typeHierarchy.addAll(safe(candidateType.getDerivedFrom()));

            Iterator<String> iter = typeHierarchy.iterator();
            while (config == null && iter.hasNext()) {
                config = matchingConfigurations.get(iter.next());
            }
        }
        return config;
    }

    private boolean validateTemplateMatch(V abstractTemplate, R candidate, T candidateType, LocationResources locationResources,
            MatchingConfiguration matchingConfiguration) {
        // check that the node root properties matches the filters defined on the MatchingConfigurations.
        Map<String, List<IMatchPropertyConstraint>> configuredFilters = matchingConfiguration == null ? null : matchingConfiguration.getProperties();
        if (!isValidTemplatePropertiesMatch(abstractTemplate.getProperties(), candidate.getTemplate().getProperties(), candidateType.getProperties(),
                configuredFilters)) {
            return false;
        }

        // Overridable matching logic.
        return typeSpecificMatching(abstractTemplate, candidate, candidateType, locationResources, matchingConfiguration);
    }

    /**
     * Allow definition of an optional specific matching logic based on the concrete extension of this class. For example node matching also perform
     * capability-based matching.
     * 
     * @param abstractTemplate The template to match.
     * @param candidate The candidate template as provide by the admin.
     * @param candidateType The type of the candidate template.
     * @param locationResources The location resources to get tosca types.
     * @param matchingConfiguration The (optional) matching configuration.
     * @return true if the candidate is a valid match for the template, false if not.
     */
    protected boolean typeSpecificMatching(V abstractTemplate, R candidate, T candidateType, LocationResources locationResources,
            MatchingConfiguration matchingConfiguration) {
        return true;
    }

    /**
     * Add filters ent/ICSARRepositorySearchService.java from the matching configuration to the node filter that will be applied for matching only if a value is
     * specified on the configuration template.
     *
     * @param templatePropertyValues The properties values from the template to match.
     * @param candidatePropertyValues The values defined on the Location Template.
     * @param propertyDefinitions The properties definitions associated with the node.
     * @param configuredFilters The filtering map (based on constraints) from matching configuration, other properties fall backs to an equal constraint/filter.
     */
    protected boolean isValidTemplatePropertiesMatch(Map<String, AbstractPropertyValue> templatePropertyValues,
            Map<String, AbstractPropertyValue> candidatePropertyValues, Map<String, PropertyDefinition> propertyDefinitions,
            Map<String, List<IMatchPropertyConstraint>> configuredFilters) {
        // We perform matching on every property that is defined on the candidate (admin node) and that has a value defined in the topology.
        for (Map.Entry<String, AbstractPropertyValue> candidateValueEntry : safe(candidatePropertyValues).entrySet()) {
            List<IMatchPropertyConstraint> filter = safe(configuredFilters).get(candidateValueEntry.getKey());
            AbstractPropertyValue templatePropertyValue = templatePropertyValues.get(candidateValueEntry.getKey());

            // For now we support matching only on scalar properties.
            if (candidateValueEntry.getValue() != null && candidateValueEntry.getValue() instanceof ScalarPropertyValue && templatePropertyValue != null
                    && templatePropertyValue instanceof ScalarPropertyValue) {
                try {
                    IPropertyType<?> toscaType = ToscaTypes.fromYamlTypeName(propertyDefinitions.get(candidateValueEntry.getKey()).getType());
                    if (filter == null) { // If no filter is defined then process matching using an equal constraint.
                        filter = Lists.newArrayList(new EqualConstraint());
                    }
                    // set the constraint value and add it to the node filter
                    for (IMatchPropertyConstraint constraint : filter) {
                        constraint.setConstraintValue(toscaType, ((ScalarPropertyValue) candidateValueEntry.getValue()).getValue());
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
     * @param abstractTemplate The template to match.
     * @param candidateType The type of the candidate.
     * @return True if the candidate type matches the template type, false if not.
     */
    private boolean isCandidateTypeValid(V abstractTemplate, T candidateType) {
        return candidateType.getElementId().equals(abstractTemplate.getType()) || safe(candidateType.getDerivedFrom()).contains(abstractTemplate.getType());
    }
}