package alien4cloud.rest.model;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A filtered search request object for components
 * 
 * @author 'Igor Ngouagna'
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class FilteredSearchRequest extends BasicSearchRequest {
    private Map<String, String[]> filters;

    public FilteredSearchRequest(String query, Integer from, Integer size, Map<String, String[]> filters) {
        super(query, from, size);
        this.filters = filters;
    }
}