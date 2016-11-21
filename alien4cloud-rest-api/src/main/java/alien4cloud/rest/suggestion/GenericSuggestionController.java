package alien4cloud.rest.suggestion;

import java.io.IOException;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.suggestions.services.SuggestionService;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({ "/rest/v1/suggestions", "/rest/latest/suggestions" })
@Api
public class GenericSuggestionController {

    @Resource
    private SuggestionService suggestionService;

    /**
     * Initialize the default configured suggestions
     */
    @ApiOperation(value = "Initialize the default configured suggestions")
    @RequestMapping(value = "/init", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Void> initDefaultSuggestions() throws IOException {
        suggestionService.loadDefaultSuggestions();
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Create a suggestion entry")
    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Void> createSuggestion(@RequestBody CreateSuggestionEntryRequest request) {
        suggestionService.createSuggestionEntry(request.getEsIndex(), request.getEsType(), request.getSuggestions(), request.getTargetElementId(),
                request.getTargetProperty());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Get matched suggestions
     *
     * @param suggestionId the suggestionEntry id.
     * @param input the input entered by the user, if the input is empty then get all values.
     * @param limit the maximum number of suggestions to return
     * @return The suggestion who match the input entered by the user.
     */
    @ApiOperation(value = "Get matched suggestions", notes = "Returns the matched suggestions.")
    @RequestMapping(value = "/{suggestionId:.+}/values", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String[]> getMatchedSuggestions(@PathVariable String suggestionId, @RequestParam(required = false) String input,
            @RequestParam(required = false) Integer limit) {
        if (limit == null || limit <= 0) {
            limit = Integer.MAX_VALUE;
        }
        String[] suggestions = suggestionService.getJaroWinklerMatchedSuggestions(suggestionId, input, limit);
        return RestResponseBuilder.<String[]> builder().data(suggestions).build();
    }

    /**
     * Add new suggestion value
     *
     * @param suggestionId the suggestionEntry id.
     * @param value the new suggestion value.
     * @return a rest response if the operation has succeeded
     */
    @ApiOperation(value = "Add new suggestion value")
    @RequestMapping(value = "/{suggestionId:.+}/values/{value}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Void> addSuggestionValue(@PathVariable String suggestionId, @PathVariable String value) {
        suggestionService.addSuggestionValueToSuggestionEntry(suggestionId, value);
        return RestResponseBuilder.<Void> builder().build();
    }
}
