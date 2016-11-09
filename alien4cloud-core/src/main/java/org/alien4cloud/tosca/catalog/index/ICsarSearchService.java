package org.alien4cloud.tosca.catalog.index;

import java.util.Map;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.plugin.aop.Overridable;

public interface ICsarSearchService {

    /**
     * Search for Abstract tosca types in the catalog.
     *
     * @param query The text query.
     * @param from from index.
     * @param size The size of the query.
     * @param filters Filters.
     * @return A faceted search result that provides both result data and facets.
     */
    @Overridable
    FacetedSearchResult search(String query, int from, int size, Map<String, String[]> filters);
}
