package alien4cloud.component;

import java.util.Map;

import alien4cloud.dao.model.FacetedSearchResult;
import org.alien4cloud.tosca.model.types.AbstractToscaType;

/**
 * Service to search for elements.
 */
public interface IElementSearchService {
    /**
     * Search for indexed tosca elements.
     * 
     * @param classNameToQuery The exact name of the indexed tosca element to search.
     * @param query The search query.
     * @param size Size of elements to get from es.
     * @param filters Filters.
     * @return A faceted search result that contains both the search results and the facets to help user improve the query.
     */
    FacetedSearchResult search(Class<? extends AbstractToscaType> classNameToQuery, String query, Integer size, Map<String, String[]> filters);
}
