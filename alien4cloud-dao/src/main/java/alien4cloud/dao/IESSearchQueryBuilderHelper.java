package alien4cloud.dao;

import java.util.Map;
import java.util.function.Consumer;

import alien4cloud.dao.model.FacetedSearchResult;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.mapping.FilterValuesStrategy;
import org.elasticsearch.mapping.QueryBuilderAdapter;
import org.elasticsearch.mapping.QueryHelper;

import alien4cloud.dao.model.GetMultipleDataResult;

/**
 * 
 */
public interface IESSearchQueryBuilderHelper<T> extends IESQueryBuilderHelper<T> {
    /**
     * Execute a search query using the defined query.
     *
     * @param from The start index of the search (for pagination).
     * @param size The maximum number of elements to return.
     */
    GetMultipleDataResult<T> search(int from, int size);

    /**
     * Execute a search query using the defined query with facets (aggregations).
     *
     * @param from The start index of the search (for pagination).
     * @param size The maximum number of elements to return.
     */
    FacetedSearchResult<T> facetedSearch(int from, int size);

    /**
     * Get the underlying search request builder.
     *
     * @return The underlying search request builder.
     */
    SearchRequestBuilder getSearchRequestBuilder();

    /**
     * Execute the given consumer to alter the search request builder.
     *
     * @param searchRequestBuilderConsumer the search request builder consumer to alter the search request.
     */
    IESSearchQueryBuilderHelper alterSearchRequestBuilder(Consumer<SearchRequestBuilder> searchRequestBuilderConsumer);

    /**
     * Allows to define a sort field.
     *
     * @param fieldName Name of the field to sort.
     * @param desc Descending or Ascending
     * @return this
     */
    IESSearchQueryBuilderHelper setFieldSort(String fieldName, boolean desc);

    /**
     * Add a fetch context to the query.
     *
     * @param fetchContext The fetch context to add to the query.
     */
    IESSearchQueryBuilderHelper setFetchContext(String fetchContext);

    @Override
    IESSearchQueryBuilderHelper alterQueryBuilder(QueryBuilderAdapter queryBuilderAdapter);

    @Override
    IESSearchQueryBuilderHelper setScriptFunction(String functionScore);

    @Override
    IESSearchQueryBuilderHelper setFilters(FilterBuilder... customFilter);

    @Override
    IESSearchQueryBuilderHelper setFilters(Map<String, String[]> filters, FilterBuilder... customFilters);

    @Override
    IESSearchQueryBuilderHelper setFilters(Map<String, String[]> filters, Map<String, FilterValuesStrategy> filterStrategies, FilterBuilder... customFilters);

}