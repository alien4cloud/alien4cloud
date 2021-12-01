package alien4cloud.rest.suggestion;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import alien4cloud.model.suggestion.Suggestion;
import alien4cloud.model.suggestion.SuggestionRequestContext;
import alien4cloud.security.AuthorizationUtil;
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
     * Deprecated : we keep this GET for retro compatibility, but the POST request with context should be preferred.
     * @see GenericSuggestionController#getContextualMatchedSuggestions(java.lang.String, java.lang.String, java.lang.Integer, SuggestionRequestContext)
     *
     * @param suggestionId the suggestionEntry id.
     * @param input the input entered by the user, if the input is empty then get all values.
     * @param limit the maximum number of suggestions to return
     * @return The suggestion who match the input entered by the user.
     */
    @ApiOperation(value = "Get matched suggestions", notes = "Returns the matched suggestions.")
    @RequestMapping(value = "/{suggestionId:.+}/values", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Deprecated
    public RestResponse<List<String>> getMatchedSuggestions(@PathVariable String suggestionId, @RequestParam(required = false) String input,
                                                                @RequestParam(required = false) Integer limit) {

        SuggestionRequestContext context = new SuggestionRequestContext();
        context.setUser(AuthorizationUtil.getCurrentUser());
        RestResponse<List<Suggestion>> matchedSuggestions = this.getMatchedSuggestions(suggestionId, input, limit, context);
        List<String> simpleSuggestions = matchedSuggestions.getData().stream().map(suggestion -> suggestion.getValue()).collect(Collectors.toList());
        return RestResponseBuilder.<List<String>> builder().data(simpleSuggestions).error(matchedSuggestions.getError()).build();
    }

    private RestResponse<List<Suggestion>> getMatchedSuggestions(String suggestionId, String input,
                                                                 Integer limit, SuggestionRequestContext context) {
        if (limit == null || limit <= 0) {
            limit = Integer.MAX_VALUE;
        }
        List<Suggestion> suggestions = suggestionService.getJaroWinklerMatchedSuggestions(suggestionId, input, limit, context);
        return RestResponseBuilder.<List<Suggestion>> builder().data(suggestions).build();
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
    @RequestMapping(value = "/{suggestionId:.+}/values", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<Suggestion>> getContextualMatchedSuggestions(@PathVariable String suggestionId,
                                                                          @RequestParam(required = false) String input,
                                                                          @RequestParam(required = false) Integer limit,
                                                                          @RequestBody SuggestionRequestContext context
    ) {

        context.setUser(AuthorizationUtil.getCurrentUser());
        return this.getMatchedSuggestions(suggestionId, input, limit, context);
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
