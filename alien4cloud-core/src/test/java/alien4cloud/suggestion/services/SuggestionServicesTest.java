package alien4cloud.suggestion.services;

import java.util.Arrays;
import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.common.SuggestionEntry;
import alien4cloud.suggestions.services.SuggestionService;

@Slf4j
public class SuggestionServicesTest {

    @Test
    public void testSuggestionMatching() {
        SuggestionService suggestionService = new SuggestionService();
        IGenericSearchDAO alienDAO = Mockito.mock(IGenericSearchDAO.class);
        suggestionService.setAlienDAO(alienDAO);
        SuggestionEntry suggestionEntry = new SuggestionEntry();
        suggestionEntry.setSuggestions(new HashSet<>(Arrays.asList("ubuntu", "windows xp", "kubuntu", "windows 2000", "gentoo", "mint", "debian")));
        Mockito.when(alienDAO.findById(SuggestionEntry.class, "")).thenReturn(suggestionEntry);

        String[] matches = suggestionService.getMatchedSuggestions("", "u", Integer.MAX_VALUE);
        Assert.assertEquals(1, matches.length);
        Assert.assertEquals("ubuntu", matches[0]);
        log.info("Matches for 'u': {}", Arrays.asList(matches));

        matches = suggestionService.getMatchedSuggestions("", "ub", 2);
        Assert.assertEquals(2, matches.length);
        Assert.assertEquals("ubuntu", matches[0]);
        Assert.assertEquals("kubuntu", matches[1]);
        log.info("Matches for 'ub': {}", Arrays.asList(matches));

        matches = suggestionService.getMatchedSuggestions("", "wtn d ow spp", Integer.MAX_VALUE);
        log.info("Matches for 'wtn d ow spp': {}", Arrays.asList(matches));
        Assert.assertEquals("windows xp", matches[0]);

        matches = suggestionService.getMatchedSuggestions("", "guntoo", Integer.MAX_VALUE);
        log.info("Matches for 'guntoo': {}", Arrays.asList(matches));
        Assert.assertEquals("gentoo", matches[0]);

        matches = suggestionService.getMatchedSuggestions("", "mt", Integer.MAX_VALUE);
        log.info("Matches for 'mt': {}", Arrays.asList(matches));
        Assert.assertEquals("mint", matches[0]);

        matches = suggestionService.getMatchedSuggestions("", "Windows", Integer.MAX_VALUE);
        log.info("Matches for 'Windows': {}", Arrays.asList(matches));
        Assert.assertEquals(7, matches.length);
        Assert.assertEquals("windows xp", matches[0]);
        Assert.assertEquals("windows 2000", matches[1]);

        matches = suggestionService.getMatchedSuggestions("", "", 5);
        log.info("Matches for blank: {}", Arrays.asList(matches));
        Assert.assertEquals(5, matches.length);
    }
    //
    // private void printDistance(String left, String right) {
    // StringUtils.getJaroWinklerDistance(left, right);
    // System.out.println("Distance between '" + left + "' and right '" + right + "' is " + StringUtils.getJaroWinklerDistance(left, right));
    // }
    //
    // @Test
    // public void testJarowinkler() {
    // printDistance("x86_32", "x86_3");
    // }
}
