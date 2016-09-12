package alien4cloud.dao;

import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.mapping.FilterValuesStrategy;
import org.elasticsearch.mapping.QueryHelper;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;

/**
 * A Dao that supports search and/or filter based queries.
 */
public interface IGenericSearchDAO extends IGenericIdDAO {

    /**
     * Return the dao {@link QueryHelper}
     *
     * @return
     */
    QueryHelper getQueryHelper();

    /**
     * Get the index in which a class belongs.
     *
     * @param clazz The class for which to get the index.
     * @return The name of the index in which the class lies.
     */
    String getIndexForType(Class<?> clazz);

    /**
     * Count the number of objects in the index in which a given class lies. The result is not limited to the actual given type.
     *
     * @param clazz Class of the object. This is used to retrieve the index on which to perform the count. The current query do not query only the given type
     *            but all object in the index.
     * @param query Additional query.
     * @return The number of objects in the index in which the given class lies (Note some other types may lies in the same index and be counted also.
     */
    <T> long count(Class<T> clazz, QueryBuilder query);

    /**
     * Count the number of objects in the index that matches (based on class name) the given class, search text and filters.
     *
     * @param clazz The type of data to query.
     * @param searchText The text of the search request (null to match all).
     * @param filters The filters to apply to the request.
     * @return The number of objects matching the query.
     */
    <T> long count(Class<T> clazz, String searchText, Map<String, String[]> filters);

    /**
     * Delete a data by query
     *
     * @param clazz
     * @param query
     */
    void delete(Class<?> clazz, QueryBuilder query);

    /**
     * Run a custom query on elastic search for the given class.
     *
     * @param clazz The type of data to query.
     * @param query The query to execute.
     * @return A single result.
     */
    <T> T customFind(Class<T> clazz, QueryBuilder query);

    /**
     * Run a custom query on elastic search for the given class.
     *
     * @param clazz The type of data to query.
     * @param query The query to execute.
     * @param sortBuilder the sort configuration
     * @return A single result.
     */
    <T> T customFind(Class<T> clazz, QueryBuilder query, SortBuilder sortBuilder);

    /**
     * Run a custom query on elastic search for the given class.
     *
     * @param clazz The type of data to query.
     * @param query The query to execute.
     * @return All result.
     */
    <T> List<T> customFindAll(Class<T> clazz, QueryBuilder query);

    /**
     * Run a custom query on elastic search for the given class.
     *
     * @param clazz The type of data to query.
     * @param query The query to execute.
     * @param sortBuilder the sort configuration
     * @return All result.
     */
    <T> List<T> customFindAll(Class<T> clazz, QueryBuilder query, SortBuilder sortBuilder);

    /**
     * Run a query build from a {@link QueryHelper.ISearchQueryBuilderHelper}.
     *
     * @param queryHelperBuilder The query builder that contains the query to run.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} that contains the search response.
     */
    GetMultipleDataResult<Object> search(QueryHelper.ISearchQueryBuilderHelper queryHelperBuilder, int from, int maxElements);

    /**
     * Search for data.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search or null if no filters.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} instance that contains the result data. Empty instance if no data found.
     */
    <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, int maxElements);

    /**
     * Same as {@link IGenericSearchDAO#search(Class, String, Map, int)}, but with pagination supported.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search or null if no filters.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} instance that contains the result data. Empty instance if no data found.
     * @see IGenericSearchDAO#search(Class, String, Map, int)
     */
    <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, int from, int maxElements);

    /**
     * Same as {@link IGenericSearchDAO#search(Class, String, Map, int)}, but with pagination supported.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search or null if no filters.
     * @param fetchContext A fetch context to define a partial response.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} instance that contains the result data. Empty instance if no data found.
     * @see IGenericSearchDAO#search(Class, String, Map, int)
     */
    <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, String fetchContext, int from, int maxElements);

    /**
     * Same as {@link IGenericSearchDAO#search(Class, String, Map, int)}, but with pagination supported.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search or null if no filters.
     * @param fetchContext A fetch context to define a partial response.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @param fieldSort field to sort on
     * @param sortOrder order for the sort (false = ascending or true = descending)
     * @return A {@link GetMultipleDataResult} instance that contains the result data. Empty instance if no data found.
     * @see IGenericSearchDAO#search(Class, String, Map, int)
     */
    <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter, String fetchContext,
            int from, int maxElements, String fieldSort, boolean sortOrder);

    /**
     * Same as {@link IGenericSearchDAO#search(Class, String, Map, int)}, but with pagination supported.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search or null if no filters.
     * @param customFilter The custom defined filter
     * @param fetchContext A fetch context to define a partial response.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} instance that contains the result data. Empty instance if no data found.
     * @see IGenericSearchDAO#search(Class, String, Map, int)
     */
    <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter, String fetchContext,
            int from, int maxElements);

    /**
     * Performs a search on the given indices
     *
     * @param searchIndices Indices in which to search.
     * @param classes Classes to search.
     * @param searchText The text to search for.
     * @param from start element in the search.
     * @param filters The filters for the search.
     * @param fetchContext A fetch context to define a partial response.
     * @param maxElements Maximum number of elements to get.
     * @return A {@link GetMultipleDataResult} that contains the various elements to get.
     */
    GetMultipleDataResult<Object> search(String[] searchIndices, Class<?>[] classes, String searchText, Map<String, String[]> filters, String fetchContext,
            int from, int maxElements);

    /**
     * Performs a search on the given indices
     *
     * @param searchIndices Indices in which to search.
     * @param classes Classes to search.
     * @param searchText The text to search for.
     * @param from start element in the search.
     * @param filters The filters for the search.
     * @param customFilter The custom defined filter.
     * @param fetchContext A fetch context to define a partial response.
     * @param maxElements Maximum number of elements to get.
     * @return A {@link GetMultipleDataResult} that contains the various elements to get.
     */
    GetMultipleDataResult<Object> search(String[] searchIndices, Class<?>[] classes, String searchText, Map<String, String[]> filters,
            FilterBuilder customFilter, String fetchContext, int from, int maxElements);

    /**
     * Search for data and get a list of facets if any are configured.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link FacetedSearchResult} instance that contains the result data and associated facets. Empty instance if no data found.
     */
    <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, int maxElements);

    /**
     * Same as {@link IGenericSearchDAO#facetedSearch(Class, String, Map, int)}, but with pagination supported.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search.
     * @param fetchContext A fetch context to define a partial response.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link FacetedSearchResult} instance that contains the result data and associated facets. Empty instance if no data found.
     * @see IGenericSearchDAO#facetedSearch(Class, String, Map, int)
     */
    <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, String fetchContext, int from, int maxElements);

    /**
     * Same as {@link IGenericSearchDAO#facetedSearch(Class, String, Map, int)}, but with pagination supported.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search.
     * @param customFilter The custom defined filter.
     * @param fetchContext A fetch context to define a partial response.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link FacetedSearchResult} instance that contains the result data and associated facets. Empty instance if no data found.
     * @see IGenericSearchDAO#facetedSearch(Class, String, Map, int)
     */
    <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter, String fetchContext,
            int from, int maxElements);

    /**
     * Same as {@link IGenericSearchDAO#facetedSearch(Class, String, Map, int)}, but with pagination supported.
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search.
     * @param customFilter The custom defined filter.
     * @param fetchContext A fetch context to define a partial response.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @param fieldSort field to sort on
     * @param sortOrder order for the sort (false = ascending or true = descending)
     * @return A {@link FacetedSearchResult} instance that contains the result data and associated facets. Empty instance if no data found.
     * @see IGenericSearchDAO#facetedSearch(Class, String, Map, int)
     */
    <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter, String fetchContext,
            int from, int maxElements, String fieldSort, boolean sortOrder);

    /**
     * Perform a suggestion search on a specific field.
     *
     * @param searchIndices The indices to search for.
     * @param requestedTypes The types to include in the query.
     * @param suggestFieldPath The path to the field for which to manage suggestion.
     * @param searchPrefix The value of the current prefix for suggestion.
     * @param fetchContext The fetch context to recover only the required field (Note that this should be simplified to directly use the given field...).
     * @param from The start index for suggestion (usually 0).
     * @param maxElements The maximum number of elements to retrieve for suggestion.
     * @return A {@link GetMultipleDataResult} that contains data matching the requested suggestion.
     */
    GetMultipleDataResult<Object> suggestSearch(String[] searchIndices, Class<?>[] requestedTypes, String suggestFieldPath, String searchPrefix,
            String fetchContext, int from, int maxElements);

    /**
     * find data matching the given type and filters.
     *
     * @param clazz The type of data to query.
     * @param filters The filters for the search or null if no filters.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} instance that contains the result data.
     */
    <T> GetMultipleDataResult<T> find(Class<T> clazz, Map<String, String[]> filters, int maxElements);

    /**
     * Same as {@link IGenericSearchDAO#find(Class, Map, int)}, but with pagination supported.
     *
     * @param clazz The type of data to query.
     * @param filters The filters for the search or null if no filters.
     * @param from Offset from the first result you want to fetch.
     * @param maxElements The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} instance that contains the result data.
     * @see IGenericSearchDAO#find(Class, Map, int)
     */
    <T> GetMultipleDataResult<T> find(Class<T> clazz, Map<String, String[]> filters, int from, int maxElements);

    /**
     * Get the Map of types to classes
     *
     * @return
     */
    Map<String, Class<?>> getTypesToClasses();

    /**
     * Get the Map of types to indexes
     *
     * @return
     */
    Map<String, String> getTypesToIndices();

    /**
     * Search for data. allowing filter strategy "AND, OR" when multiple values in the filter
     *
     * @param clazz The type of data to query.
     * @param searchText The search text if any.
     * @param filters The filters for the search or null if no filters.
     * @param filterStrategies A map containing {@link FilterValuesStrategy}. A key correspond to a filter key. for values, see {@link FilterValuesStrategy}
     * @param maxElements The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} instance that contains the result data. Empty instance if no data found.
     */
    <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, Map<String, FilterValuesStrategy> filterStrategies,
            int maxElements);

    /**
     * Find instances by id, only retrieve specific fields of the object.
     *
     * @param clazz The class for which to find an instance.
     * @param ids array of id of the data to find.
     * @param fetchContext The fetch context to recover only the required field (Note that this should be simplified to directly use the given field...).
     * @return List of Objects that has the given ids or null if no object matching the request is found.
     */
    <T> List<T> findByIdsWithContext(Class<T> clazz, String fetchContext, String... ids);

    /**
     * Select the list of value for the path
     *
     * @param index the index
     * @param types the types
     * @param queryBuilder the query to filter
     * @param sortOrder sort the returned result
     * @param path the path to select
     * @param from offset
     * @param size size
     * @return the list of value for the path
     */
    String[] selectPath(String index, Class<?>[] types, QueryBuilder queryBuilder, SortOrder sortOrder, String path, int from, int size);

    /**
     * Select the list of value for the path
     *
     * @param index the index
     * @param types the types
     * @param queryBuilder the query to filter
     * @param sortOrder sort the returned result
     * @param path the path to select
     * @param from offset
     * @param size size
     * @return the list of value for the path
     */
    String[] selectPath(String index, String[] types, QueryBuilder queryBuilder, SortOrder sortOrder, String path, int from, int size);

    /**
     * Create a query builder for the given class based on a match all query.
     *
     * @return an instance of IESQueryBuilderHelper for the given class.
     */
    public <T> IESQueryBuilderHelper<T> buildQuery(Class<T> clazz);

    /**
     * Create a query builder for the given class.
     *
     * @return an instance of IESQueryBuilderHelper for the given class.
     */
    public <T> IESQueryBuilderHelper<T> buildSearchQuery(Class<T> clazz, String searchQuery);

    /**
     * Create a query builder for the given class.
     *
     * @return an instance of IESQueryBuilderHelper for the given class.
     */
    public <T> IESQueryBuilderHelper<T> buildSuggestionQuery(Class<T> clazz, String prefixField, String searchQuery);
}
