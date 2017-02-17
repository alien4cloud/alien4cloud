package alien4cloud.rest.component;

import java.util.Map;

import alien4cloud.rest.model.BasicSearchRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A search request object for components
 * 
 * @author 'Igor Ngouagna'
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ComponentSearchRequest extends BasicSearchRequest {
    /* The component type to query */
    private QueryComponentType type;
    private Map<String, String[]> filters;

    public ComponentSearchRequest(QueryComponentType type, String query, Integer from, Integer size, Map<String, String[]> filters) {
        super(query, from, size);
        this.type = type;
        this.filters = filters;
    }
}