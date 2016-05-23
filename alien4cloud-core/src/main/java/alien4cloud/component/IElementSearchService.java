package alien4cloud.component;

import java.util.Map;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.components.IndexedToscaElement;

/**
 * Service to search for elements.
 */
public interface IElementSearchService {
    /**
     *
     * @param classNameToQuery
     * @param query
     * @param from
     * @param size
     * @param filters
     * @param queryAllVersions
     * @return
     */
    FacetedSearchResult search(Class<? extends IndexedToscaElement> classNameToQuery, String query, Integer from, Integer size, Map<String, String[]> filters,
            boolean queryAllVersions);
}
