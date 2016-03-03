package alien4cloud.rest.suggestion;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Resource;

import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.suggestions.services.SuggestionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.rest.model.RestResponse;

@RestController
@RequestMapping({"/rest/v1/suggestions", "/rest/latest/suggestions"})
public class GenericSuggestionController {

    @Resource
    private SuggestionService suggestionService;

    public RestResponse<String[]> getSuggestionsById(@PathVariable("index") String index, @PathVariable("type") String type, @PathVariable("path") String path,
            @RequestParam("text") String searchText) throws IOException {
        Set<String> suggestions = suggestionService.getSuggestions(searchText);
        return RestResponseBuilder.<String[]> builder().data(suggestions.toArray(new String[0])).build();
    }
}
