package alien4cloud.dao;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.dao.model.FacetedSearchResult;

import java.util.function.Function;

/**
 * Extract query results from an aggregation rather than from the query.
 */
public interface IAggregationQueryManager<T> {
    /**
     * Get the aggregation to be used for query.
     * 
     * @return The aggregation to be used for query.
     */
    AggregationBuilder getQueryAggregation();

    /**
     * Parse the results of the query aggregation to create data array.
     *
     * @param objectMapper The json mapper to parse results out of elastic search
     * @param result The result in which to inject data.
     * @param aggregation The aggregation.
     * @return An array of results.
     */
    void setData(ObjectMapper objectMapper, Function<String, Class> getClassFromType, FacetedSearchResult result, Aggregation aggregation);
}