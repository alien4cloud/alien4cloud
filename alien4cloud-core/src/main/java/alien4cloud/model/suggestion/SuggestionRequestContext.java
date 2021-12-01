package alien4cloud.model.suggestion;

import alien4cloud.security.model.User;
import lombok.*;

/**
 * The end-user context when s.he types something in a text property that is related to a suggestion.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SuggestionRequestContext {

    private SuggestionContextData data = new SuggestionContextData();

    private User user;

    private SuggestionContextType type;

}
