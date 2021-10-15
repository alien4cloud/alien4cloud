package alien4cloud.model.suggestion;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.query.FetchContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;

@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class AbstractSuggestionEntry {

    /**
     * List of values that can be suggested for the property ( for example Windows, Linux, Mac OS etc ...)
     */
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Set<String> suggestions = Sets.newHashSet();

    /**
     * A suggestion can also be a key/value pair (actually value/description).
     */
    @NestedObject(nestedClass = Suggestion.class)
    private List<Suggestion> complexSuggestions = Lists.newArrayList();

    /**
     * Optionally delegates call to a plugin bean to get suggestions.
     * Format is : pluginId:beanName
     */
    private String suggestionHookPlugin;

    /**
     * Define what should be done when user input is not found in suggested values :
     * <ul>
     *   <li><b>Strict</b>: You can only choose one in results.</li>
     *   <li><b>Ask</b>: Ask for the user what to do (current default mode : a popup ask the user if s.he want to add it in registry).</li>
     *   <li><b>Add</b>: Add the new entry without asking the user (not recommended).</li>
     *   <li><b>Accept</b>: Don't ask for nothing, accept the value for the property, but don't add it to registry.</li>
     * </ul>
     */
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private SuggestionPolicy suggestionPolicy = SuggestionPolicy.Accept;

    public Collection<Suggestion> getBuiltSuggestions() {
        List<Suggestion> result = Lists.newArrayList();
        for (String s : suggestions) {
            result.add(new Suggestion(s, ""));
        }
        if (complexSuggestions != null) {
            result.addAll(complexSuggestions);
        }
        return result;
    }

    @Id
    public abstract String getId();

    public abstract void setId(String id);

}
