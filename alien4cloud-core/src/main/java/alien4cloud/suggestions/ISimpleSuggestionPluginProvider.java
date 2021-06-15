package alien4cloud.suggestions;

import alien4cloud.model.suggestion.SuggestionRequestContext;

import java.util.Collection;

/**
 * The contract for a plugin bean that can suggest some values to the end-user.
 */
public interface ISimpleSuggestionPluginProvider extends ISuggestionPluginProvider {

    Collection<String> getSuggestions(String input, SuggestionRequestContext context);

}
