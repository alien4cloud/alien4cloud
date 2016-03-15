package alien4cloud.suggestions.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.SuggestionEntry;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.utils.YamlParserUtil;

import com.google.common.collect.Maps;

@Slf4j
@Component
public class SuggestionService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /* The Levenshtein distance is a string metric for measuring the difference between two sequences. */
    private static final int MAX_LEVENSHTEIN = 2;

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
                suggestionEntry.generateId();
                if (!isSuggestionExist(suggestionEntry)) {
                    alienDAO.save(suggestionEntry);
                }
            }
        }
    }

    /**
     * Iterate on default suggestions to update all assosiate property definition.
     */
    public void setAllSuggestionIDOnPropertyDefinition() {
        List<SuggestionEntry> suggestionEntries = getAllSuggestionEntries();
        if (suggestionEntries != null && !suggestionEntries.isEmpty()) {
            for (SuggestionEntry suggestionEntry : suggestionEntries) {
                setSuggestionIdOnPropertyDefinition(suggestionEntry);
            }
        }
    }

    /**
     * Add the suggestion ID of the new suggestionEntry to the appropriate propertyDefinition.
     * 
     * @param suggestionEntry entry of suggestion
     */
    public void setSuggestionIdOnPropertyDefinition(SuggestionEntry suggestionEntry) {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("elementId", new String[] { suggestionEntry.getTargetElementId() });
        Class<? extends IndexedInheritableToscaElement> targetClass = (Class<? extends IndexedInheritableToscaElement>) alienDAO.getTypesToClasses().get(
                suggestionEntry.getEsType());
        GetMultipleDataResult<? extends IndexedInheritableToscaElement> result = alienDAO.find(targetClass, filters, Integer.MAX_VALUE);
        if (result.getData() != null && result.getData().length > 0) {
            for (IndexedInheritableToscaElement targetElement : result.getData()) {
                PropertyDefinition propertyDefinition = targetElement.getProperties().get(suggestionEntry.getTargetProperty());
                if (propertyDefinition == null) {
                    throw new NotFoundException("Property [" + suggestionEntry.getTargetProperty() + "] not found for element ["
                            + suggestionEntry.getTargetElementId() + "]");
                } else {
                    switch (propertyDefinition.getType()) {
                    case ToscaType.STRING:
                        propertyDefinition.setSuggestionId(suggestionEntry.getId());
                        alienDAO.save(targetElement);
                        break;
                    case ToscaType.LIST:
                    case ToscaType.MAP:
                        PropertyDefinition entrySchema = propertyDefinition.getEntrySchema();
                        if (entrySchema != null) {
                            entrySchema.setSuggestionId(suggestionEntry.getId());
                            alienDAO.save(targetElement);
                        } else {
                            throw new InvalidArgumentException("Cannot suggest a list / map type with no entry schema definition");
                        }
                        break;
                    default:
                        throw new InvalidArgumentException(propertyDefinition.getType()
                                + " cannot be suggested, only property of type string list or map can be suggested");
                    }
                }
            }
        }
    }

    public void addSuggestionValueToSuggestionEntry(String suggestionId, String newValue) {
        SuggestionEntry suggestion = alienDAO.findById(SuggestionEntry.class, suggestionId);
        if (suggestion == null) {
            throw new NotFoundException("Suggestion entry [" + suggestionId + "] cannot be found");
        }
        // TODO: should check the format of new value
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

    private static class MatchedSuggestion {
        Double priority;
        String value;

        public MatchedSuggestion(Double priority, String value) {
            this.priority = priority;
            this.value = value;
        }
    }

    private MatchedSuggestion getMatch(String suggestion, String normalizedValue) {
        // Compute the match score between the suggestion and the normalized value
        String normalizedSuggestion = normalizeTextForMatching(suggestion);
        int distanceBetweenValueAndSuggestion = StringUtils.getLevenshteinDistance(normalizedSuggestion, normalizedValue);
        Double levenshteinScore = null;
        if (distanceBetweenValueAndSuggestion <= MAX_LEVENSHTEIN) {
            levenshteinScore = ((double) (normalizedSuggestion.length() - distanceBetweenValueAndSuggestion)) / (double) normalizedSuggestion.length();
        }
        Double indexOfScore = null;
        int indexOfSearchedText = normalizedSuggestion.indexOf(normalizedValue);
        if (indexOfSearchedText == 0) {
            // Start with the searched value has more priorities than contains the value
            indexOfScore = ((double) normalizedValue.length()) / (double) normalizedSuggestion.length();
        } else if (indexOfSearchedText > 0) {
            // Contains the value
            indexOfScore = ((double) (normalizedValue.length() - 1)) / (double) normalizedSuggestion.length();
        }
        Double score;
        if (indexOfScore == null) {
            // Returns null if not considered as a match
            score = levenshteinScore;
        } else {
            if (levenshteinScore == null) {
                score = indexOfScore;
            } else {
                score = Math.max(indexOfScore, levenshteinScore);
            }
        }
        if (score == null) {
            return null;
        } else {
            return new MatchedSuggestion(score, suggestion);
        }
    }

    /**
     * Get the suggestion with less difference between value and suggestions.
     *
     * @param value value to match for suggestion
     * @param suggestionId id of the suggestion
     * @param limit the number of match to consider
     * @return the suggestion with less difference between value and suggestions.
     */
    public String[] getMatchedSuggestions(String suggestionId, String value, int limit) {
        Set<String> allSuggestions = getSuggestions(suggestionId);
        String normalizedValue = normalizeTextForMatching(value);
        if (limit > allSuggestions.size()) {
            limit = allSuggestions.size();
        }
        if (StringUtils.isEmpty(normalizedValue)) {
            // Finish prematurely the algorithm as the searched value is empty
            String[] matches = new String[limit];
            Iterator<String> allSuggestionsIterator = allSuggestions.iterator();
            for (int i = 0; i < limit; i++) {
                matches[i] = allSuggestionsIterator.next();
            }
            return matches;
        }
        // The priority queue is here is to see what is the value that matches the suggestion the most
        PriorityQueue<MatchedSuggestion> matchedSuggestions = new PriorityQueue<>(10, Collections.reverseOrder(new Comparator<MatchedSuggestion>() {
            @Override
            public int compare(MatchedSuggestion o1, MatchedSuggestion o2) {
                return o1.priority.compareTo(o2.priority);
            }
        }));
        // Process matched text with its score
        for (String suggestion : allSuggestions) {
            MatchedSuggestion matchedSuggestion = getMatch(suggestion, normalizedValue);
            if (matchedSuggestion != null) {
                matchedSuggestions.add(matchedSuggestion);
            }
        }
        if (limit > matchedSuggestions.size()) {
            limit = matchedSuggestions.size();
        }
        String[] results = new String[limit];
        Iterator<MatchedSuggestion> matchedSuggestionIterator = matchedSuggestions.iterator();
        for (int i = 0; i < limit; i++) {
            results[i] = matchedSuggestionIterator.next().value;
        }
        return results;
    }

    /**
     * Get all suggestions by suggestion ID.
     *
     * @param suggestionId id of the suggestion
     * @return all suggestions of the {@link SuggestionEntry}.
     */
    public Set<String> getSuggestions(String suggestionId) {
        SuggestionEntry suggestionEntry = alienDAO.findById(SuggestionEntry.class, suggestionId);
        if (suggestionEntry == null) {
            throw new NotFoundException("Suggestion entry [" + suggestionId + "] cannot be found");
        }
        return suggestionEntry.getSuggestions();
    }

    /**
     * Check if a suggestionEntry already exist.
     *
     * @param suggestionEntry entry of suggestion
     * @return a boolean indicating if the suggestionEntry exists.
     */
    public boolean isSuggestionExist(SuggestionEntry suggestionEntry) {
        SuggestionEntry suggestion = alienDAO.findById(SuggestionEntry.class, suggestionEntry.getId());
        return suggestion != null;
    }

    /**
     * Get all suggestionEntry
     * 
     * @return all suggestion entries
     */
    public List<SuggestionEntry> getAllSuggestionEntries() {
        return alienDAO.customFindAll(SuggestionEntry.class, null);
    }

    public void setAlienDAO(IGenericSearchDAO alienDAO) {
        this.alienDAO = alienDAO;
    }
}
