package alien4cloud.suggestions.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.google.common.collect.Sets;
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
import com.google.common.io.Closeables;

@Slf4j
@Component
public class SuggestionService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /* The Levenshtein distance is a string metric for measuring the difference between two sequences. */
    private final int MAX_LEVENSHTEIN = 2;

    /**
     * This method load the defaults suggestions to ES.
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
        for (SuggestionEntry suggestionEntry : suggestionEntries) {
            setSuggestionIdOnPropertyDefinition(suggestionEntry);
        }
    }

    /**
     * Add the suggestion ID of the new suggestionEntry to the appropriate propertyDefinition.
     * @param suggestionEntry
     */
    public void setSuggestionIdOnPropertyDefinition(SuggestionEntry suggestionEntry) {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("elementId", new String[]{suggestionEntry.getTargetElementId()});
        Class<? extends IndexedInheritableToscaElement> targetClass = (Class<? extends IndexedInheritableToscaElement>) alienDAO.getTypesToClasses()
                .get(suggestionEntry.getEsType());
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

    /**
     * Get the suggestion with less difference between value and suggestions.
     *
     * @param value
     * @param suggestionId
     * @return the suggestion with less difference between value and suggestions.
     */
    public Set<String> getMatchedSuggestions(String suggestionId, String value) {
        Set<String> suggestions = getSuggestions(suggestionId);
        Set<String> matchedSuggestions = Sets.newHashSet();
        String formatedValue = value.toLowerCase().replace(" ", "");
        for (String suggestion : suggestions) {
            if (StringUtils.getLevenshteinDistance(suggestion.toLowerCase().replace(" ", ""), formatedValue) <= MAX_LEVENSHTEIN) {
                matchedSuggestions.add(suggestion);
            }
        }
        return matchedSuggestions;
    }

    /**
     * Get all suggestions by suggestion ID.
     *
     * @param suggestionId
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
     * @param suggestionEntry
     * @return a boolean indicating if the suggestionEntry exists.
     */
    public boolean isSuggestionExist(SuggestionEntry suggestionEntry) {
        SuggestionEntry suggestion = alienDAO.findById(SuggestionEntry.class, suggestionEntry.getId());
        if (suggestion != null) {
            return true;
        }
        return false;
    }

    /**
     * Get all suggestionEntry
     * @return all suggestion entries
     */
    public List<SuggestionEntry> getAllSuggestionEntries() {
        return alienDAO.customFindAll(SuggestionEntry.class, null);
    }
}
