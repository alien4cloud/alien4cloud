package alien4cloud.suggestions.services;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.model.suggestion.AbstractSuggestionEntry;
import alien4cloud.model.suggestion.Suggestion;
import alien4cloud.model.suggestion.SuggestionEntry;
import alien4cloud.model.suggestion.SuggestionRequestContext;
import alien4cloud.plugin.exception.MissingPluginException;
import alien4cloud.suggestions.IComplexSuggestionPluginProvider;
import alien4cloud.suggestions.ISimpleSuggestionPluginProvider;
import alien4cloud.suggestions.ISuggestionPluginProvider;
import com.google.common.collect.Lists;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FilterDefinition;
import org.alien4cloud.tosca.model.definitions.NodeFilter;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.ValidValuesConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.YamlParserUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SuggestionService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;

    @Inject
    private SuggestionPluginRegistry suggestionPluginRegistry;

    /* The Levenshtein distance is a string metric for measuring the difference between two sequences. */
    private static final double MIN_JAROWINKLER = 0.0;

    /**
     * This method load the defaults suggestions to ES.
     * 
     * @throws IOException
     */
    @PostConstruct
    public void loadDefaultSuggestions() throws IOException {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("suggestion-configuration.yml")) {
            SuggestionEntry[] suggestions = YamlParserUtil.parse(input, SuggestionEntry[].class);
            for (SuggestionEntry suggestionEntry : suggestions) {
                createSuggestionEntry(suggestionEntry);
            }
        }
    }

    /**
     * Iterate on default suggestions to update all associate property definition.
     */
    public void setAllSuggestionIdOnPropertyDefinition() {
        List<AbstractSuggestionEntry> suggestionEntries = getAllSuggestionEntries();
        if (suggestionEntries != null && !suggestionEntries.isEmpty()) {
            for (AbstractSuggestionEntry suggestionEntry : suggestionEntries) {
                if (suggestionEntry instanceof SuggestionEntry) {
                    try {
                        setSuggestionIdOnPropertyDefinition((SuggestionEntry) suggestionEntry);
                    } catch(Exception e) {
                        log.warn("Not able to apply suggestion entry <{}> due to the following exception : {}", suggestionEntry.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    private AbstractSuggestionEntry checkProperty(String nodePrefix, String propertyName, String propertyTextValue,
            Class<? extends AbstractInheritableToscaType> type, String elementId, ParsingContext context) {
        AbstractSuggestionEntry suggestionEntry = getSuggestionEntry(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, type.getSimpleName().toLowerCase(), elementId,
                propertyName);
        if (suggestionEntry != null) {
            PriorityQueue<SuggestionService.MatchedSuggestion> similarValues = getJaroWinklerMatchedSuggestions(suggestionEntry.getBuiltSuggestions(),
                    propertyTextValue, 0.8);
            if (!similarValues.isEmpty()) {
                // Has some similar values in the system already
                SuggestionService.MatchedSuggestion mostMatched = similarValues.poll();
                if (!mostMatched.getValue().getValue().equals(propertyTextValue)) {
                    // If user has entered a property value not the same as the most matched in the system
                    ParsingErrorLevel level;
                    if (mostMatched.getPriority() == 1.0) {
                        // It's really identical if we take out all white spaces and lower / upper case
                        level = ParsingErrorLevel.WARNING;
                    } else {
                        // It's pretty similar
                        level = ParsingErrorLevel.INFO;
                        // Add suggestion anyway
                        addSuggestionValueToSuggestionEntry(suggestionEntry.getId(), propertyTextValue);
                    }
                    context.getParsingErrors()
                            .add(new ParsingError(level, ErrorCode.POTENTIAL_BAD_PROPERTY_VALUE, null, null, null, null, "At path [" + nodePrefix + "."
                                    + propertyName + "] existing value [" + mostMatched.getValue().getValue() + "] is very similar to [" + propertyTextValue + "]"));
                }
            } else {
                // Not similar add suggestion
                addSuggestionValueToSuggestionEntry(suggestionEntry.getId(), propertyTextValue);
            }
        }
        return suggestionEntry;
    }

    private void checkProperties(String nodePrefix, Map<String, AbstractPropertyValue> propertyValueMap, Class<? extends AbstractInheritableToscaType> type,
            String elementId, ParsingContext context) {
        if (MapUtils.isNotEmpty(propertyValueMap)) {
            for (Map.Entry<String, AbstractPropertyValue> propertyValueEntry : propertyValueMap.entrySet()) {
                String propertyName = propertyValueEntry.getKey();
                AbstractPropertyValue propertyValue = propertyValueEntry.getValue();
                if (propertyValue instanceof ScalarPropertyValue) {
                    String propertyTextValue = ((ScalarPropertyValue) propertyValue).getValue();
                    checkProperty(nodePrefix, propertyName, propertyTextValue, type, elementId, context);
                }
            }
        }
    }

    public void postProcessSuggestionFromArchive(ParsingResult<ArchiveRoot> parsingResult) {
        ArchiveRoot archiveRoot = parsingResult.getResult();
        ParsingContext context = parsingResult.getContext();
        if (archiveRoot.hasToscaTopologyTemplate()) {
            Topology topology = archiveRoot.getTopology();
            Map<String, NodeTemplate> nodeTemplateMap = topology.getNodeTemplates();
            if (MapUtils.isEmpty(nodeTemplateMap)) {
                return;
            }
            for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : nodeTemplateMap.entrySet()) {
                NodeTemplate nodeTemplate = nodeTemplateEntry.getValue();
                String nodeName = nodeTemplateEntry.getKey();
                if (MapUtils.isNotEmpty(nodeTemplate.getProperties())) {
                    checkProperties(nodeName, nodeTemplate.getProperties(), NodeType.class, nodeTemplate.getType(), context);
                }
                Map<String, Capability> capabilityMap = nodeTemplate.getCapabilities();
                if (MapUtils.isNotEmpty(capabilityMap)) {
                    for (Map.Entry<String, Capability> capabilityEntry : capabilityMap.entrySet()) {
                        String capabilityName = capabilityEntry.getKey();
                        Capability capability = capabilityEntry.getValue();
                        if (MapUtils.isNotEmpty(capability.getProperties())) {
                            checkProperties(nodeName + ".capabilities." + capabilityName, capability.getProperties(), CapabilityType.class,
                                    capability.getType(), context);
                        }
                    }
                }
                Map<String, RelationshipTemplate> relationshipTemplateMap = nodeTemplate.getRelationships();
                if (MapUtils.isNotEmpty(relationshipTemplateMap)) {
                    for (Map.Entry<String, RelationshipTemplate> relationshipEntry : relationshipTemplateMap.entrySet()) {
                        String relationshipName = relationshipEntry.getKey();
                        RelationshipTemplate relationship = relationshipEntry.getValue();
                        if (MapUtils.isNotEmpty(relationship.getProperties())) {
                            checkProperties(nodeName + ".relationships." + relationshipName, relationship.getProperties(), RelationshipType.class,
                                    relationship.getType(), context);
                        }
                    }
                }
            }
        }
        if (archiveRoot.hasToscaTypes()) {
            Map<String, NodeType> allNodeTypes = archiveRoot.getNodeTypes();
            if (MapUtils.isNotEmpty(allNodeTypes)) {
                for (Map.Entry<String, NodeType> nodeTypeEntry : allNodeTypes.entrySet()) {
                    NodeType nodeType = nodeTypeEntry.getValue();
                    if (nodeType.getRequirements() != null && !nodeType.getRequirements().isEmpty()) {
                        for (RequirementDefinition requirementDefinition : nodeType.getRequirements()) {
                            NodeFilter nodeFilter = requirementDefinition.getNodeFilter();
                            if (nodeFilter != null) {
                                Map<String, FilterDefinition> capabilitiesFilters = nodeFilter.getCapabilities();
                                if (MapUtils.isNotEmpty(capabilitiesFilters)) {
                                    for (Map.Entry<String, FilterDefinition> capabilityFilterEntry : capabilitiesFilters.entrySet()) {
                                        FilterDefinition filterDefinition = capabilityFilterEntry.getValue();
                                        for (Map.Entry<String, List<PropertyConstraint>> constraintEntry : filterDefinition.getProperties().entrySet()) {
                                            List<PropertyConstraint> constraints = constraintEntry.getValue();
                                            checkPropertyConstraints("node_filter.capabilities", CapabilityType.class, capabilityFilterEntry.getKey(),
                                                    constraintEntry.getKey(), constraints, context);
                                        }
                                    }
                                }
                                // FIXME check also the value properties filter of a node filter
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a new suggestion entry
     * 
     * @param type the targeted type
     * @param initialValues the initial values
     * @param elementId element id
     * @param propertyName property's name
     */
    public void createSuggestionEntry(String index, Class<? extends AbstractToscaType> type, Set<String> initialValues, String elementId, String propertyName) {
        createSuggestionEntry(index, type.getSimpleName().toLowerCase(), initialValues, elementId, propertyName);
    }

    /**
     * Create a new suggestion entry
     *
     * @param type the targeted type
     * @param initialValues the initial values
     * @param elementId element id
     * @param propertyName property's name
     */
    public void createSuggestionEntry(String index, String type, Set<String> initialValues, String elementId, String propertyName) {
        SuggestionEntry suggestionEntry = new SuggestionEntry();
        suggestionEntry.setEsIndex(index);
        suggestionEntry.setEsType(type);
        suggestionEntry.setSuggestions(initialValues);
        suggestionEntry.setTargetElementId(elementId);
        suggestionEntry.setTargetProperty(propertyName);
        createSuggestionEntry(suggestionEntry);
    }

    /**
     * Create a new simple suggestion entry.
     */
    public void createSuggestionEntry(SuggestionEntry suggestionEntry) {
        AbstractSuggestionEntry existingSuggestionEntry = alienDAO.findById(AbstractSuggestionEntry.class, suggestionEntry.getId());
        if (existingSuggestionEntry == null) {
            alienDAO.save(suggestionEntry);
        } else {
            existingSuggestionEntry.setSuggestionPolicy(suggestionEntry.getSuggestionPolicy());
            existingSuggestionEntry.setSuggestionHookPlugin(suggestionEntry.getSuggestionHookPlugin());
            suggestionEntry.getSuggestions().forEach(s -> {
                if (!existingSuggestionEntry.getSuggestions().contains(s)) {
                    existingSuggestionEntry.getSuggestions().add(s);
                }
            });
            suggestionEntry.getComplexSuggestions().forEach(suggestion -> {
                if (!existingSuggestionEntry.getComplexSuggestions().stream().anyMatch(existing -> existing.getValue().equals(suggestion.getValue()))) {
                    existingSuggestionEntry.getComplexSuggestions().add(suggestion);
                }
            });
            alienDAO.save(existingSuggestionEntry);
        }
        setSuggestionIdOnPropertyDefinition(suggestionEntry);
    }

    private void checkPropertyConstraints(String prefix, Class<? extends AbstractInheritableToscaType> type, String elementId, String propertyName,
            List<PropertyConstraint> constraints, ParsingContext context) {
        if (constraints != null && !constraints.isEmpty()) {
            for (PropertyConstraint propertyConstraint : constraints) {
                if (propertyConstraint instanceof EqualConstraint) {
                    EqualConstraint equalConstraint = (EqualConstraint) propertyConstraint;
                    String valueToCheck = equalConstraint.getEqual();
                    if (checkProperty(prefix, propertyName, valueToCheck, type, elementId, context) == null) {
                        createSuggestionEntry(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, CapabilityType.class, Sets.newHashSet(valueToCheck), elementId,
                                propertyName);
                    }
                } else if (propertyConstraint instanceof ValidValuesConstraint) {
                    ValidValuesConstraint validValuesConstraint = (ValidValuesConstraint) propertyConstraint;
                    if (validValuesConstraint.getValidValues() != null && !validValuesConstraint.getValidValues().isEmpty()) {
                        AbstractSuggestionEntry foundSuggestion = null;
                        for (String valueToCheck : validValuesConstraint.getValidValues()) {
                            foundSuggestion = checkProperty(prefix, propertyName, valueToCheck, type, elementId, context);
                            if (foundSuggestion == null) {
                                // No suggestion exists don't need to check any more for other values
                                break;
                            }
                        }
                        if (foundSuggestion == null) {
                            createSuggestionEntry(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, CapabilityType.class,
                                    Sets.newHashSet(validValuesConstraint.getValidValues()), elementId, propertyName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Add the suggestion ID of the new suggestionEntry to the appropriate propertyDefinition.
     * 
     * @param suggestionEntry entry of suggestion
     */
    private void setSuggestionIdOnPropertyDefinition(SuggestionEntry suggestionEntry) {
        Class<? extends AbstractInheritableToscaType> targetClass = (Class<? extends AbstractInheritableToscaType>) alienDAO.getTypesToClasses()
                .get(suggestionEntry.getEsType());

        // FIXME what if targetClass is null ?
        Object array = toscaTypeSearchService.findAll(targetClass, suggestionEntry.getTargetElementId());
        if (array != null) {
            int length = Array.getLength(array);
            for (int i = 0; i < length; i++) {
                AbstractInheritableToscaType targetElement = ((AbstractInheritableToscaType) Array.get(array, i));
                PropertyDefinition propertyDefinition = targetElement.getProperties().get(suggestionEntry.getTargetProperty());
                if (propertyDefinition == null) {
                    throw new NotFoundException(
                            "Property [" + suggestionEntry.getTargetProperty() + "] not found for element [" + suggestionEntry.getTargetElementId() + "]");
                } else {
                    switch (propertyDefinition.getType()) {
                    case ToscaTypes.VERSION:
                    case ToscaTypes.STRING:
                        propertyDefinition.setSuggestionId(suggestionEntry.getId());
                        propertyDefinition.setSuggestionPolicy(suggestionEntry.getSuggestionPolicy().name());
                        alienDAO.save(targetElement);
                        break;
                    case ToscaTypes.LIST:
                    case ToscaTypes.MAP:
                        PropertyDefinition entrySchema = propertyDefinition.getEntrySchema();
                        if (entrySchema != null) {
                            entrySchema.setSuggestionId(suggestionEntry.getId());
                            entrySchema.setSuggestionPolicy(suggestionEntry.getSuggestionPolicy().name());
                            alienDAO.save(targetElement);
                        } else {
                            throw new InvalidArgumentException("Cannot suggest a list / map type with no entry schema definition");
                        }
                        break;
                    default:
                        throw new InvalidArgumentException(
                                propertyDefinition.getType() + " cannot be suggested, only property of type string list or map can be suggested");
                    }
                }
            }
        }
    }

    public void addSuggestionValueToSuggestionEntry(String suggestionId, String newValue) {
        AbstractSuggestionEntry suggestion = alienDAO.findById(AbstractSuggestionEntry.class, suggestionId);
        if (suggestion == null) {
            throw new NotFoundException("Suggestion entry [" + suggestionId + "] cannot be found");
        }
        if (StringUtils.isNotEmpty(suggestion.getSuggestionHookPlugin())) {
            // a suggestion that is related to a plugin bean don't need to store it's values.
            return;
        }
        // TODO: should check the format of new value
        if (suggestion.getSuggestions().contains(newValue)) {
            return;
        }
        suggestion.getSuggestions().add(newValue);
        alienDAO.save(suggestion);
    }

    private String normalizeTextForMatching(String value) {
        if (value == null) {
            return "";
        }
        String noWhiteSpace = value.replace(" ", "");
        return noWhiteSpace.toLowerCase();
    }

    public static class MatchedSuggestion {
        Double priority;
        Suggestion value;

        public MatchedSuggestion(Double priority, Suggestion value) {
            this.priority = priority;
            this.value = value;
        }

        public Double getPriority() {
            return priority;
        }

        public Suggestion getValue() {
            return value;
        }
    }

    private MatchedSuggestion getMatch(Suggestion suggestion, String normalizedValue, double minJarowinkler) {
        // Compute the match score between the suggestion and the normalized value
        String normalizedSuggestion = normalizeTextForMatching(suggestion.getValue());
        double distance = StringUtils.getJaroWinklerDistance(normalizedValue, normalizedSuggestion);
        if (StringUtils.isNotEmpty(suggestion.getDescription())) {
            String normalizedSuggestionDesc = normalizeTextForMatching(suggestion.getDescription());
            double distanceToDescription = StringUtils.getJaroWinklerDistance(normalizedValue, normalizedSuggestionDesc);
            if (distanceToDescription > distance) {
                distance = distanceToDescription;
            }
        }
        if (distance == 1 && !normalizedValue.equals(normalizedSuggestion)) {
            distance = 0.999;
        }
        if (distance > minJarowinkler) {
            return new MatchedSuggestion(distance, suggestion);
        } else {
            return null;
        }
    }

    public PriorityQueue<MatchedSuggestion> getJaroWinklerMatchedSuggestions(Collection<Suggestion> allSuggestions, String input, double minJaroWinkler) {
        String normalizedInput = normalizeTextForMatching(input);
        // The priority queue is here is to see what is the value that matches the suggestion the most
        PriorityQueue<MatchedSuggestion> matchedSuggestions = new PriorityQueue<>(10, Collections.reverseOrder(new Comparator<MatchedSuggestion>() {
            @Override
            public int compare(MatchedSuggestion o1, MatchedSuggestion o2) {
                return o1.priority.compareTo(o2.priority);
            }
        }));
        // Process matched text with its score
        for (Suggestion suggestion : allSuggestions) {
            MatchedSuggestion matchedSuggestion = getMatch(suggestion, normalizedInput, minJaroWinkler);
            if (matchedSuggestion != null) {
                matchedSuggestions.add(matchedSuggestion);
            }
        }
        return matchedSuggestions;
    }

    public List<Suggestion> getJaroWinklerMatchedSuggestions(String suggestionId, String input, int limit) {
        return this.getJaroWinklerMatchedSuggestions(suggestionId, input, limit, null);
    }

        /**
         * Get the suggestions that might match the input value.
         *
         * @param input value to match for suggestion
         * @param suggestionId id of the suggestion
         * @param limit the number of match to consider
         * @return the suggestions ordered by the most match.
         */
    public List<Suggestion> getJaroWinklerMatchedSuggestions(String suggestionId, String input, int limit, SuggestionRequestContext context) {
        log.info("Current context is: {}", context);
        Collection<Suggestion> allSuggestions = getSuggestions(suggestionId, input, context);
        if (limit > allSuggestions.size()) {
            limit = allSuggestions.size();
        }
        if (StringUtils.isBlank(input)) {
            // Finish prematurely the algorithm as the searched value is empty
            List<Suggestion> matches = Lists.newArrayList();
            //String[] matches = new String[limit];
            Iterator<Suggestion> allSuggestionsIterator = allSuggestions.iterator();
            for (int i = 0; i < limit; i++) {
                matches.add(allSuggestionsIterator.next());
                //matches[i] = allSuggestionsIterator.next();
            }
            return matches;
        }
        PriorityQueue<MatchedSuggestion> matchedSuggestions = getJaroWinklerMatchedSuggestions(allSuggestions, input, MIN_JAROWINKLER);
        if (limit > matchedSuggestions.size()) {
            limit = matchedSuggestions.size();
        }
        List<Suggestion> results = Lists.newArrayList();
        for (int i = 0; i < limit; i++) {
            results.add(matchedSuggestions.poll().value);
        }
        return results;
    }

    /**
     * Get all suggestions by suggestion ID.
     *
     * @param suggestionId id of the suggestion
     * @return all suggestions of the {@link SuggestionEntry}.
     */
    public Collection<Suggestion> getSuggestions(String suggestionId, String input, SuggestionRequestContext context) {
        AbstractSuggestionEntry suggestionEntry = alienDAO.findById(AbstractSuggestionEntry.class, suggestionId);
        if (suggestionEntry == null) {
            throw new NotFoundException("Suggestion entry [" + suggestionId + "] cannot be found");
        }
        List<Suggestion> suggestions = Lists.newArrayList();
        // TODO change to getSuggestion()
        suggestions.addAll(suggestionEntry.getBuiltSuggestions());
        Collection<Suggestion> pluginSuggestions = getSuggestionFromHookPlugin(suggestionEntry, input, context);
        if (pluginSuggestions != null) {
            suggestions.addAll(pluginSuggestions);
        }
        return suggestions;
        //return CollectionUtils.merge(suggestionEntry.getSuggestions(), getSuggestionFromHookPlugin(suggestionEntry));
    }

    public Collection<Suggestion> getSuggestionFromHookPlugin(AbstractSuggestionEntry suggestionEntry, String input, SuggestionRequestContext context) {
        if (suggestionEntry.getSuggestionHookPlugin() != null) {
            String[] pluginIdentifiers = suggestionEntry.getSuggestionHookPlugin().split(":");
            if (pluginIdentifiers.length != 2) {
                log.warn("Can't parse suggestion_hook_plugin for suggestion {} : {} (should be pluginId:beanName)", suggestionEntry.getId(), suggestionEntry.getSuggestionHookPlugin());
            } else {
                try {
                    ISuggestionPluginProvider suggestionPlugin = suggestionPluginRegistry.getPluginBean(pluginIdentifiers[0], pluginIdentifiers[1]);
                    if (suggestionPlugin instanceof ISimpleSuggestionPluginProvider) {
                        Collection<String> suggestionsStrs = ((ISimpleSuggestionPluginProvider)suggestionPlugin).getSuggestions(input, context);
                        // TODO: how to do this just with streams ?
                        List<Suggestion> result = Lists.newArrayList();
                        suggestionsStrs.stream().forEach(s -> result.add(new Suggestion(s, null)));
                        return result;
                    } else if (suggestionPlugin instanceof IComplexSuggestionPluginProvider) {
                        return ((IComplexSuggestionPluginProvider)suggestionPlugin).getSuggestions(input, context);
                    }

                } catch(MissingPluginException mpe) {
                    log.warn("Can't find plugin for suggestions {} : {} ({})", suggestionEntry.getId(), suggestionEntry.getSuggestionHookPlugin(), mpe.getMessage());
                } catch(Exception e) {
                    log.error("An error occurred while getting suggestions from plugin {} : {}", suggestionEntry.getId(), suggestionEntry.getSuggestionHookPlugin(), e);
                }
            }
        }
        return null;
    }

    /**
     * Check if a suggestionEntry already exist.
     *
     * @param suggestionEntry entry of suggestion
     * @return a boolean indicating if the suggestionEntry exists.
     */
    public boolean isSuggestionExist(AbstractSuggestionEntry suggestionEntry) {
        AbstractSuggestionEntry suggestion = alienDAO.findById(AbstractSuggestionEntry.class, suggestionEntry.getId());
        return suggestion != null;
    }

    public AbstractSuggestionEntry getSuggestionEntry(String index, String type, String elementId, String property) {
        return alienDAO.findById(AbstractSuggestionEntry.class, SuggestionEntry.generateId(index, type, elementId, property));
    }

    /**
     * Get all suggestionEntries, attention this method do not return suggested values
     * 
     * @return all suggestion entries without their values
     */
    private List<AbstractSuggestionEntry> getAllSuggestionEntries() {
        GetMultipleDataResult<AbstractSuggestionEntry> result = alienDAO.search(AbstractSuggestionEntry.class, null, null, FetchContext.SUMMARY, 0,
                Integer.MAX_VALUE);
        if (result.getData() != null && result.getData().length > 0) {
            return Arrays.asList(result.getData());
        } else {
            return new ArrayList<>();
        }
    }

    public void setAlienDAO(IGenericSearchDAO alienDAO) {
        this.alienDAO = alienDAO;
    }
}
