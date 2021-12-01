package alien4cloud.suggestions.services;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import alien4cloud.model.suggestion.AbstractSuggestionEntry;
import alien4cloud.model.suggestion.Suggestion;
import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.suggestion.SuggestionEntry;

@Slf4j
public class SuggestionServicesTest {
    @Test
    public void testSuggestionMatching() {
        SuggestionService suggestionService = new SuggestionService();
        IGenericSearchDAO alienDAO = Mockito.mock(IGenericSearchDAO.class);
        suggestionService.setAlienDAO(alienDAO);
        SuggestionEntry suggestionEntry = new SuggestionEntry();
        suggestionEntry.setSuggestions(new HashSet<>(Arrays.asList("ubuntu", "windows xp", "kubuntu", "windows 2000", "gentoo", "mint", "debian")));
        Mockito.when(alienDAO.findById(AbstractSuggestionEntry.class, "")).thenReturn(suggestionEntry);

        List<Suggestion> matches = suggestionService.getJaroWinklerMatchedSuggestions("", "u", Integer.MAX_VALUE);
        Assert.assertEquals(2, matches.size());
        Assert.assertEquals("ubuntu", matches.get(0).getValue());
        Assert.assertEquals("kubuntu", matches.get(1).getValue());
        log.info("Matches for 'u': {}", Arrays.asList(matches));

        matches = suggestionService.getJaroWinklerMatchedSuggestions("", "ub", 2);
        Assert.assertEquals(2, matches.size());
        Assert.assertEquals("ubuntu", matches.get(0).getValue());
        Assert.assertEquals("kubuntu", matches.get(1).getValue());
        log.info("Matches for 'ub': {}", Arrays.asList(matches));

        matches = suggestionService.getJaroWinklerMatchedSuggestions("", "wtn d ow spp", Integer.MAX_VALUE);
        log.info("Matches for 'wtn d ow spp': {}", Arrays.asList(matches));
        Assert.assertEquals("windows xp", matches.get(0).getValue());

        matches = suggestionService.getJaroWinklerMatchedSuggestions("", "guntoo", Integer.MAX_VALUE);
        log.info("Matches for 'guntoo': {}", Arrays.asList(matches));
        Assert.assertEquals("gentoo", matches.get(0).getValue());

        matches = suggestionService.getJaroWinklerMatchedSuggestions("", "mt", Integer.MAX_VALUE);
        log.info("Matches for 'mt': {}", Arrays.asList(matches));
        Assert.assertEquals("mint", matches.get(0).getValue());

        matches = suggestionService.getJaroWinklerMatchedSuggestions("", "Windows", Integer.MAX_VALUE);
        log.info("Matches for 'Windows': {}", Arrays.asList(matches));
        Assert.assertEquals(7, matches.size());
        Assert.assertEquals("windows xp", matches.get(0).getValue());
        Assert.assertEquals("windows 2000", matches.get(1).getValue());

        matches = suggestionService.getJaroWinklerMatchedSuggestions("", "", 5);
        log.info("Matches for blank: {}", Arrays.asList(matches));
        Assert.assertEquals(5, matches.size());
    }
}
