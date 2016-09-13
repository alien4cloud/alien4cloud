package alien4cloud.dao;

import java.util.Map;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.mapping.FilterValuesStrategy;
import org.elasticsearch.mapping.ISearchBuilderAdapter;
import org.elasticsearch.mapping.QueryBuilderAdapter;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;

/**
 * 
 */
public interface IESSearchQueryBuilderHelper<T> extends IESQueryBuilderHelper<T> {

    /**
     * Execute a search query and get a single element or null if no elements can be found.
     * Note that the query may have multiple valid results but only a single one will be returned. Based on the query syntax and sort parameters different
     * results may be returned every time.
     * 
     * @return the first matching instance or null if none can be found.
     */
    T find();

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
     * Execute a search query using the defined query with facets (aggregations) and use the provided IAggregationQueryManager to fetch data from an aggregation
     * rather than query result.
     * 
     * @param aggregationQueryManager The aggregation query manager that manages the query through aggregation.
     * @return The facet search result that contains data from the aggregation as well as other facets.
     */
    FacetedSearchResult facetedSearch(IAggregationQueryManager aggregationQueryManager);

    /**
     * Get the underlying search request builder.
     *
     * @return The underlying search request builder.
     */
    SearchRequestBuilder getSearchRequestBuilder();

    /**
     * Execute the given consumer to alter the search request builder.
     *
     * @param searchBuilderAdapter the search request builder adapter to alter the search request.
     */
    IESSearchQueryBuilderHelper<T> alterSearchRequestBuilder(ISearchBuilderAdapter searchBuilderAdapter);

    /**
     * Allows to define a sort field.
     *
     * @param fieldName Name of the field to sort.
     * @param desc Descending or Ascending
     * @return this
     */
    IESSearchQueryBuilderHelper<T> setFieldSort(String fieldName, boolean desc);

    /**
     * Add a fetch context to the query.
     *
     * @param fetchContext The fetch context to add to the query.
     */
    IESSearchQueryBuilderHelper<T> setFetchContext(String fetchContext);

    /**
     * Apply the fetch context to the given aggregation (BUT DOES NOT add it to the query).
     *
     * @param fetchContext The fetch context to add to the aggregation.
     * @param topHitsBuilder The top hits aggregation builder on which to add fetch context include and excludes.
     * @return The search query builder helper with the top
     */
    IESSearchQueryBuilderHelper<T> setFetchContext(String fetchContext, TopHitsBuilder topHitsBuilder);

    @Override
    IESSearchQueryBuilderHelper<T> alterQueryBuilder(QueryBuilderAdapter queryBuilderAdapter);

    @Override
    IESSearchQueryBuilderHelper<T> setScriptFunction(String functionScore);

    @Override
    IESSearchQueryBuilderHelper<T> setFilters(FilterBuilder... customFilter);

    @Override
    IESSearchQueryBuilderHelper<T> setFilters(Map<String, String[]> filters, FilterBuilder... customFilters);

    @Override
    IESSearchQueryBuilderHelper<T> setFilters(Map<String, String[]> filters, Map<String, FilterValuesStrategy> filterStrategies,
            FilterBuilder... customFilters);

}