package alien4cloud.rest.suggestion;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Resource;

import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.suggestions.services.SuggestionService;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.rest.model.RestResponse;

@RestController
@RequestMapping({"/rest/v1/suggestions", "/rest/latest/suggestions"})
public class GenericSuggestionController {

    @Resource
    private SuggestionService suggestionService;

    /**
     * Get all suggestions of an {@link alien4cloud.model.common.SuggestionEntry}.
     *
     * @param suggestionId The suggestion id.
     * @return All suggestions of an {@link alien4cloud.model.common.SuggestionEntry}.
     */
    @ApiOperation(value = "Get matched suggestions", notes = "Returns the matched suggestions.")
    @RequestMapping(value = "/{suggestionId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String[]> get(@PathVariable String suggestionId) {
        Set<String> suggestions = suggestionService.getSuggestions(suggestionId);
        return RestResponseBuilder.<String[]> builder().data(suggestions.toArray(new String[0])).build();
    }

    /**
     * Get matched suggestion
     *
     * @param suggestionId the suggestionEntry id.
     * @param value the value capture by the user.
     * @return The suggestion who match the value capture by the user.
     */
    @ApiOperation(value = "Get matched suggestions", notes = "Returns the matched suggestions.")
    @RequestMapping(value = "/{suggestionId:.+}/matched/{value}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String[]> getMatchedSuggestions(@PathVariable String suggestionId, @PathVariable String value) {
        Set<String> suggestions = suggestionService.getMatchedSuggestions(suggestionId, value);
        return RestResponseBuilder.<String[]> builder().data(suggestions.toArray(new String[0])).build();
    }

}
