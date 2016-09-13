package alien4cloud.dao;

import java.util.Map;
import java.util.function.Consumer;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.mapping.FilterValuesStrategy;
import org.elasticsearch.mapping.QueryBuilderAdapter;

/**
 * Adapted version from elastic search mapping to add class management features.
 */
public interface IESQueryBuilderHelper<T> {
    /**
     * Perform a count request on the given indices.
     *
     * @return The count response.
     */
    long count();

    /**
     * Build a search query.
     *
     * @return an instance of search query builder helper
     */
    IESSearchQueryBuilderHelper<T> prepareSearch();

    /**
     * Execute the given consumer to alter the query builder.
     *
     * @param queryBuilderAdapter the query builder adapter to alter the query.
     * @return current builder instance.
     */
    IESQueryBuilderHelper<T> alterQueryBuilder(QueryBuilderAdapter queryBuilderAdapter);

    /**
     * Set a script function to use for scoring
     *
     * @param functionScore The function to use for scoring.
     * @return current builder instance.
     */
    IESQueryBuilderHelper<T> setScriptFunction(String functionScore);

    /**
     * Set filters from user provided filters.
     *
     * @param customFilter user provided filters.
     * @return current instance.
     */
    IESQueryBuilderHelper<T> setFilters(FilterBuilder... customFilter);

    /**
     * Add filters to the current query.
     *
     * @param filters The filters to add the the query based on annotation defined filters (as a filtered query).
     * @param customFilters user provided filters to add (using and clause) to the annotation based filters.
     * @return current instance.
     */
    IESQueryBuilderHelper<T> setFilters(Map<String, String[]> filters, FilterBuilder... customFilters);

    /**
     * Add filters to the current query.
     *
     * @param filters The filters to add the the query based on annotation defined filters (as a filtered query).
     * @param filterStrategies The filter strategies to apply to filters.
     * @param customFilters user provided filters to add (using and clause) to the annotation based filters.
     * @return current instance.
     */
    IESQueryBuilderHelper<T> setFilters(Map<String, String[]> filters, Map<String, FilterValuesStrategy> filterStrategies, FilterBuilder... customFilters);
}
