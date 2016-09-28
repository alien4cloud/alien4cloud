package org.alien4cloud.tosca.catalog.index;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Resource;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import alien4cloud.common.AlienConstants;
import alien4cloud.dao.FilterUtil;
import alien4cloud.dao.IAggregationQueryManager;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.FetchContext;
import lombok.SneakyThrows;

/**
 * This abstract class allows to search tosca indexed elements (Csar, AbstractToscaType, Topology) as they all follow the same search query logic.
 */
public abstract class AbstractToscaIndexSearchService<T> {
    @Resource(name = "alien-es-dao")
    protected IGenericSearchDAO alienDAO;

    public FacetedSearchResult search(Class<? extends T> clazz, String query, Integer size, Map<String, String[]> filters) {
        TopHitsBuilder topHitAggregation = AggregationBuilders.topHits("highest_version").setSize(1)
                .addSort(new FieldSortBuilder("nestedVersion.majorVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.minorVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.incrementalVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.qualifier").order(SortOrder.DESC).missing("_first"));

        AggregationBuilder aggregation = AggregationBuilders.terms("query_aggregation").field(getAggregationField()).size(size)
                .subAggregation(topHitAggregation);

        FacetedSearchResult<? extends T> searchResult = alienDAO.buildSearchQuery(clazz, query)
                .setFilters(FilterUtil.singleKeyFilter(filters, "workspace", AlienConstants.GLOBAL_WORKSPACE_ID)).prepareSearch()
                .setFetchContext(FetchContext.SUMMARY, topHitAggregation).facetedSearch(new IAggregationQueryManager() {
                    @Override
                    public AggregationBuilder getQueryAggregation() {
                        return aggregation;
                    }

                    @Override
                    @SneakyThrows({ IOException.class })
                    public void setData(ObjectMapper objectMapper, Function getClassFromType, FacetedSearchResult result, Aggregation aggregation) {
                        List<Object> resultData = Lists.newArrayList();
                        List<String> resultTypes = Lists.newArrayList();
                        if (aggregation == null) {
                            result.setData(getArray(0));
                            result.setTypes(new String[0]);
                        }
                        for (Terms.Bucket bucket : ((Terms) aggregation).getBuckets()) {
                            TopHits topHits = bucket.getAggregations().get("highest_version");
                            for (SearchHit hit : topHits.getHits()) {
                                resultTypes.add(hit.getType());
                                resultData.add(
                                        objectMapper.readValue(hit.getSourceAsString(), ((Function<String, Class>) getClassFromType).apply(hit.getType())));
                            }
                        }

                        result.setData(resultData.toArray(getArray(resultData.size())));
                        result.setTypes(resultTypes.toArray(new String[resultTypes.size()]));
                        result.setFrom(0);
                        result.setTo(resultData.size());
                        if (size == Integer.MAX_VALUE || resultData.size() < size) {
                            result.setTotalResults(resultData.size());
                        } else {
                            // just to show that there is more results to fetch but iteration is not possible through aggregations.
                            result.setTotalResults(size + 1);
                        }
                    }
                });

        return searchResult;
    }

    protected abstract String getAggregationField();

    protected abstract T[] getArray(int size);
}
