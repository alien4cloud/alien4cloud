package alien4cloud.dao;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Resource;

import com.google.common.collect.ObjectArrays;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery.ScoreMode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.mapping.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.missing.InternalMissing;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.dao.model.FacetedSearchFacet;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.ElasticSearchUtil;
import alien4cloud.utils.MapUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Elastic search dao that manages search operations.
 *
 * @author luc boutier
 */
@Slf4j
public abstract class ESGenericSearchDAO extends ESGenericIdDAO implements IGenericSearchDAO {
    @Resource
    private ElasticSearchClient esClient;
    @Resource
    private QueryHelper queryHelper;

    @Resource
    private IESMetaPropertiesSearchContextBuilder metaPropertiesSearchHelper;

    @Override
    public <T> long count(Class<T> clazz, QueryBuilder query) {
        //String indexName = getIndexForType(clazz);
        String[] indexName = getIndexForType(clazz);
        //String typeName = MappingBuilder.indexTypeFromClass(clazz);
        //SearchRequestBuilder countRequestBuilder = getClient().prepareSearch(indexName).setTypes(typeName).setSize(0);
        SearchRequestBuilder countRequestBuilder = getClient().prepareSearch(indexName).setSize(0);
        if (query != null) {
            countRequestBuilder.setQuery(query);
        }
        if (log.isDebugEnabled()) {
            log.debug("Counting for <{}> using query: <{}>", clazz, query.toString());
        }
        return countRequestBuilder.execute().actionGet().getHits().getTotalHits();
    }

    @Override
    public <T> long count(Class<T> clazz, String searchText, Map<String, String[]> filters) {
        return buildSearchQuery(clazz, searchText).setFilters(filters).count();
    }

    @Override
    public void delete(Class<?> clazz, QueryBuilder query) {
        String[] indexName = getIndexForType(clazz);
        //String indexName = getIndexForType(clazz);
        String typeName = MappingBuilder.indexTypeFromClass(clazz);

        // get all elements and then use a bulk delete to remove data.
        //SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexName).setTypes(getTypesFromClass(clazz)).setQuery(query)
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexName).setQuery(query)
                .setFetchSource(false);
        searchRequestBuilder.setFrom(0).setSize(1000);
        SearchResponse response = searchRequestBuilder.execute().actionGet();

        while (somethingFound(response)) {
            BulkRequestBuilder bulkRequestBuilder = getClient().prepareBulk().setRefreshPolicy(RefreshPolicy.IMMEDIATE);

            for (int i = 0; i < response.getHits().getHits().length; i++) {
                String id = response.getHits().getHits()[i].getId();
                String respIndex = response.getHits().getHits()[i].getIndex();
                //bulkRequestBuilder.add(getClient().prepareDelete(indexName, typeName, id));
                bulkRequestBuilder.add(getClient().prepareDelete(respIndex, TYPE_NAME, id));
            }

            bulkRequestBuilder.execute().actionGet();

            if (response.getHits().getTotalHits() == response.getHits().getHits().length) {
                response = null;
            } else {
                response = searchRequestBuilder.execute().actionGet();
            }
        }
    }

    private String[] getIndicesFromClasses (Class[] clazzes) {
       List<String> result = new ArrayList<String>();
       for (Class clazz : clazzes) {
          result.addAll (Arrays.asList(getIndexForType(clazz)));
       }
       return result.toArray(new String[result.size()]);
    }

    @SneakyThrows({ IOException.class })
    private <T> List<T> doCustomFind(Class<T> clazz, QueryBuilder query, QueryBuilder filter, SortBuilder sortBuilder, int size) {
        //String indexName = getIndexForType(clazz);
        String[] indexName = getIndexForType(clazz);
        if (size > 10000) {
           size = 10000;
        }
        if (log.isDebugEnabled()) {
            log.debug("doCustomFind for <{}> size:{}", clazz, size);
        } else if (log.isTraceEnabled()) {
            log.trace("doCustomFind for <{}> size:{} using query: <{}> and filter: <{}>", clazz, size, (query != null) ? query.toString() : "", (filter != null) ? filter.toString() : "");
        }
        //SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexName).setTypes(getTypesFromClass(clazz)).setSize(size);
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexName).setSize(size);
        if (query != null) {
            searchRequestBuilder.setQuery(query);
        }
        if (filter != null) {
            searchRequestBuilder.setPostFilter(filter);
        }
        if (sortBuilder != null) {
            searchRequestBuilder.addSort(sortBuilder);
        }
        SearchResponse response = searchRequestBuilder.execute().actionGet();
        if (!somethingFound(response)) {
            return null;
        } else {
            List<T> hits = Lists.newArrayList();
            for (int i = 0; i < response.getHits().getHits().length; i++) {
                hits.add(hitToObject(response.getHits().getAt(i)));
            }
            return hits;
        }
    }

    @Override
    public <T> T customFind(Class<T> clazz, QueryBuilder query) {
        return customFind(clazz, query, null);
    }

    @Override
    public <T> T customFind(Class<T> clazz, QueryBuilder query, SortBuilder sortBuilder) {
        List<T> results = doCustomFind(clazz, query, null, sortBuilder, 1);
        if (results == null || results.isEmpty()) {
            return null;
        } else {
            return results.iterator().next();
        }
    }

    @Override
    public <T> List<T> customFilterAll(Class<T> clazz, QueryBuilder filter) {
        return doCustomFind(clazz, null, filter, null, Integer.MAX_VALUE);
    }

    @Override
    public <T> List<T> customFindAll(Class<T> clazz, QueryBuilder query) {
        return customFindAll(clazz, query, null);
    }

    @Override
    public <T> List<T> customFindAll(Class<T> clazz, QueryBuilder query, SortBuilder sortBuilder) {
        return doCustomFind(clazz, query, null, sortBuilder, Integer.MAX_VALUE);
    }

    @Override
    public <T> GetMultipleDataResult<T> find(Class<T> clazz, Map<String, String[]> filters, int maxElements) {
        return find(clazz, filters, 0, maxElements);
    }

    @Override
    public <T> GetMultipleDataResult<T> find(Class<T> clazz, Map<String, String[]> filters, int from, int maxElements) {
        return search(clazz, null, filters, from, maxElements);
    }

    @Override
    public GetMultipleDataResult<Object> search(QueryHelper.ISearchQueryBuilderHelper queryHelperBuilder, int from, int maxElements) {
        if (maxElements > 10000) {
           maxElements = 10000;
        }
        return toGetMultipleDataResult(Object.class, queryHelperBuilder.execute(from, maxElements), from);
    }

    @Override
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, int maxElements) {
        return search(clazz, searchText, filters, 0, maxElements);
    }

    @Override
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, int from, int maxElements) {
        return search(clazz, searchText, filters, null, from, maxElements);
    }

    @Override
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, String fetchContext, int from,
            int maxElements) {
        return search(clazz, searchText, filters, null, fetchContext, from, maxElements);
    }

    @Override
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, QueryBuilder customFilter,
            String fetchContext, int from, int maxElements) {
        return search(clazz, searchText, filters, customFilter, fetchContext, from, maxElements, null, null, false);
    }

    @Override
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, QueryBuilder customFilter,
            String fetchContext, int from, int maxElements, String fieldSort, String fieldType, boolean sortOrder) {
        IESSearchQueryBuilderHelper<T> searchQueryBuilderHelper = getSearchBuilderHelper(clazz, searchText, filters, customFilter, fetchContext, fieldSort,
                fieldType, sortOrder);
        if (maxElements > 10000) {
           maxElements = 10000;
        }
        if (log.isDebugEnabled()) {
            log.debug("Searching for <{}> from {} size {} using searchText: <{}> and filters <{}>", clazz.getSimpleName(), from, maxElements, searchText, filters);
        }
        return searchQueryBuilderHelper.search(from, maxElements);
    }

    @Override
    public GetMultipleDataResult<Object> search(String[] searchIndices, Class<?>[] classes, String searchText, Map<String, String[]> filters,
            String fetchContext, int from, int maxElements) {
        return search(searchIndices, classes, searchText, filters, null, fetchContext, from, maxElements);
    }

    @Override
    public GetMultipleDataResult<Object> search(String[] searchIndices, Class<?>[] classes, String searchText, Map<String, String[]> filters,
            QueryBuilder customFilter, String fetchContext, int from, int maxElements) {
        if (maxElements > 10000) {
          maxElements = 10000;
        }
        if (log.isDebugEnabled()) {
            log.debug("Searching for {}<{}> from {} size {} using searchText: <{}> and filters <{}>", searchIndices, classes, from, maxElements, searchText, filters);
        }
        //SearchResponse searchResponse = queryHelper.buildQuery(searchText).types(classes).filters(filters, customFilter).prepareSearch(searchIndices)
        SearchResponse searchResponse = queryHelper.buildQuery(searchText).types(classes).filters(filters, customFilter)
                .prepareSearch(getIndicesFromClasses(classes))
                .fetchContext(fetchContext).execute(from, maxElements);
        return toGetMultipleDataResult(Object.class, searchResponse, from);
    }

    @Override
    public <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, int maxElements) {
        return facetedSearch(clazz, searchText, filters, null, 0, maxElements);
    }

    @Override
    public <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, String fetchContext, int from,
            int maxElements) {
        return facetedSearch(clazz, searchText, filters, null, fetchContext, from, maxElements);
    }

    @Override
    public <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, QueryBuilder customFilter,
            String fetchContext, int from, int maxElements) {
        return facetedSearch(clazz, searchText, filters, customFilter, fetchContext, from, maxElements, null, null, false);
    }

    @Override
    public <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, QueryBuilder customFilter,
            String fetchContext, int from, int maxElements, String fieldSort, String fieldType, boolean sortOrder) {

        if (log.isDebugEnabled()) {
            log.debug("facetedSearch for <{}> from {} size {} using searchText: <{}> and filters <{}>", clazz, from, maxElements, searchText, filters);
        }
        IESSearchQueryBuilderHelper<T> searchQueryBuilderHelper = getSearchBuilderHelper(clazz, searchText, filters, customFilter, fetchContext, fieldSort,
                fieldType, sortOrder);
        if (maxElements > 10000) {
           maxElements = 10000;
        }
        return searchQueryBuilderHelper.facetedSearch(from, maxElements);
    }

    @Override
    public GetMultipleDataResult<Object> suggestSearch(String[] searchIndices, Class<?>[] requestedTypes, String suggestFieldPath, String searchPrefix,
            String fetchContext, int from, int maxElements) {
        if (maxElements > 10000) {
          maxElements = 10000;
        }
        //SearchResponse searchResponse = queryHelper.buildQuery(suggestFieldPath, searchPrefix).types(requestedTypes).prepareSearch(searchIndices)
        SearchResponse searchResponse = queryHelper.buildQuery(suggestFieldPath, searchPrefix).types(requestedTypes)
                .prepareSearch(getIndicesFromClasses(requestedTypes))
                .fetchContext(fetchContext).execute(from, maxElements);

        return toGetMultipleDataResult(Object.class, searchResponse, from);
    }

    @Override
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters,
            Map<String, FilterValuesStrategy> filterStrategies, int maxElements) {
        if (maxElements > 10000) {
          maxElements = 10000;
        }
        if (log.isDebugEnabled()) {
            log.debug("Searching for <{}> size {} using searchText: <{}> and filters <{}>", clazz, maxElements, searchText, filters);
        }
        return buildSearchQuery(clazz, searchText).setFilters(filters, filterStrategies).prepareSearch().search(0, maxElements);
    }

    /**
     * Convert a SearchResponse into a {@link GetMultipleDataResult} including json deserialization.
     *
     * @param searchResponse The actual search response from elastic-search.
     * @param from The start index of the search request.
     * @return A {@link GetMultipleDataResult} instance that contains de-serialized data.
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows({ IOException.class })
    public <T> GetMultipleDataResult<T> toGetMultipleDataResult(Class<T> clazz, SearchResponse searchResponse, int from) {
        // return an empty object if no data has been found in elastic search.
        if (!somethingFound(searchResponse)) {
            return new GetMultipleDataResult<T>(new String[0], (T[]) Array.newInstance(clazz, 0));
        }

        GetMultipleDataResult<T> finalResponse = new GetMultipleDataResult<T>();
        fillMultipleDataResult(clazz, searchResponse, finalResponse, from, true);

        return finalResponse;
    }

    /**
     * Convert a SearchResponse into a list of objects (json deserialization.)
     *
     * @param searchResponse The actual search response from elastic-search.
     * @param clazz The type of objects to de-serialize.
     * @return A list of instances that contains de-serialized data.
     */
    @SneakyThrows({ IOException.class })
    public <T> List<T> toGetListOfData(SearchResponse searchResponse, Class<T> clazz) {
        // return null if no data has been found in elastic search.
        if (!somethingFound(searchResponse)) {
            return null;
        }

        List<T> result = new ArrayList<>();

        for (int i = 0; i < searchResponse.getHits().getHits().length; i++) {
            result.add(hitToObject(clazz, searchResponse.getHits().getAt(i)));
        }

        return result;
    }

    private <T> void fillMultipleDataResult(Class<T> clazz, SearchResponse searchResponse, GetMultipleDataResult<T> finalResponse, int from,
            boolean managePagination) throws IOException {
        if (managePagination) {
            int to = from + searchResponse.getHits().getHits().length - 1;
            finalResponse.setFrom(from);
            finalResponse.setTo(to);
            finalResponse.setTotalResults(searchResponse.getHits().getTotalHits());
            finalResponse.setQueryDuration(searchResponse.getTook().getMillis());
        }

        String[] resultTypes = new String[searchResponse.getHits().getHits().length];

        T[] resultData = (T[]) Array.newInstance(clazz, resultTypes.length);
        for (int i = 0; i < resultTypes.length; i++) {
            SearchHit hit = searchResponse.getHits().getAt(i);
            //resultTypes[i] = hit.getType();
            resultTypes[i] = hit.getIndex();
            resultData[i] = hitToObject(hit);
        }
        finalResponse.setData(resultData);

        finalResponse.setTypes(resultTypes);
    }

    public <T> T hitToObject(SearchHit hit) throws IOException {
        //return hitToObject((Class<? extends T>) getClassFromType(hit.getType()), hit);
        return hitToObject((Class<? extends T>) getClassFromType(hit.getIndex()), hit);
    }

    public <T> T hitToObject(Class<T> clazz, SearchHit hit) throws IOException {
        T obj = (T) getJsonMapper().readValue(hit.getSourceAsString(), clazz);
        Field generatedId = getClassTogeneratedIdFields().get(clazz);
        if (generatedId != null) {
            try {
                generatedId.set(obj, hit.getId());
            } catch (IllegalAccessException e) {
                log.error("Failed to set id from elastic to declared generated id field.", e);
            }
        }
        return obj;
    }

    private <T> IESSearchQueryBuilderHelper<T> getSearchBuilderHelper(Class<T> clazz, String searchText, Map<String, String[]> filters,
            QueryBuilder customFilter, String fetchContext, String fieldSort, String fieldType, boolean sortOrder) {
        IESSearchQueryBuilderHelper<T> builderHelper = buildSearchQuery(clazz, searchText).setFilters(filters, customFilter)
                .alterQueryBuilder(queryBuilder -> QueryBuilders.functionScoreQuery(queryBuilder,
                           ScoreFunctionBuilders.fieldValueFactorFunction("alienScore").missing(1))
                        .scoreMode(ScoreMode.MULTIPLY).boostMode(CombineFunction.MULTIPLY))
                .prepareSearch().setFetchContext(fetchContext).setFieldSort(fieldSort, fieldType, sortOrder);

        return builderHelper;
    }

    private <T> QueryBuilderAdapter queryBuilderAdapter() {
        return queryBuilder -> QueryBuilders.functionScoreQuery(queryBuilder, ScoreFunctionBuilders.fieldValueFactorFunction("alienScore").missing(1))
                .scoreMode(ScoreMode.MULTIPLY).boostMode(CombineFunction.MULTIPLY);
    }

    private boolean somethingFound(final SearchResponse searchResponse) {
        if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().getHits() == null
                || searchResponse.getHits().getHits().length == 0) {
            return false;
        }
        return true;
    }

    @Override
    public <T> List<T> findByIdsWithContext(Class<T> clazz, String fetchContext, String... ids) {

        // get the fetch mpContext for the given type and apply it to the search
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();
        SourceFetchContext sourceFetchContext = getMappingBuilder().getFetchSource(clazz.getName(), fetchContext);
        if (sourceFetchContext != null) {
            includes.addAll(sourceFetchContext.getIncludes());
            excludes.addAll(sourceFetchContext.getExcludes());
        } else {
            getLog().warn("Unable to find fetch mpContext <" + fetchContext + "> for class <" + clazz.getName() + ">. It will be ignored.");
        }

        String[] inc = includes.isEmpty() ? null : includes.toArray(new String[includes.size()]);
        String[] exc = excludes.isEmpty() ? null : excludes.toArray(new String[excludes.size()]);

        if (log.isDebugEnabled()) {
            log.debug("findByIdsWithContext for <{}> fetchContext {} ids: <{}>", clazz, fetchContext, ids);
        }

        // TODO: correctly manage "from" and "size"
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(getIndexForType(clazz))
                .setQuery(QueryBuilders.idsQuery().addIds(ids)).setFetchSource(inc, exc).setSize(20);
        //        .setQuery(QueryBuilders.idsQuery(MappingBuilder.indexTypeFromClass(clazz)).addIds(ids)).setFetchSource(inc, exc).setSize(20);

        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        return toGetListOfData(searchResponse, clazz);
    }

    @Override
    public String[] selectPath(String index, Class<?>[] types, QueryBuilder queryBuilder, SortOrder sortOrder, String path, int from, int size) {
        String[] esTypes = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            esTypes[i] = MappingBuilder.indexTypeFromClass(types[i]);
        }
        return doSelectPath(index, esTypes, queryBuilder, sortOrder, path, from, size);
    }

    @Override
    public String[] selectPath(String index, String[] types, QueryBuilder queryBuilder, SortOrder sortOrder, String path, int from, int size) {
        return doSelectPath(index, types, queryBuilder, sortOrder, path, from, size);
    }

    @SneakyThrows({ IOException.class })
    private String[] doSelectPath(String index, String[] types, QueryBuilder queryBuilder, SortOrder sortOrder, String path, int from, int size) {
        if (size > 10000) {
           size = 10000;
        }
        //SearchRequestBuilder searchRequestBuilder = esClient.getClient().prepareSearch(index);
        SearchRequestBuilder searchRequestBuilder = esClient.getClient().prepareSearch(types);
        searchRequestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH).setQuery(queryBuilder).setSize(size).setFrom(from);
        searchRequestBuilder.setFetchSource(path, null);
        //searchRequestBuilder.setTypes(types);
        if (sortOrder != null) {
            searchRequestBuilder.addSort(SortBuilders.fieldSort(path).order(sortOrder));
        }
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        if (ElasticSearchUtil.isResponseEmpty(searchResponse)) {
            return new String[0];
        } else {
            String[] results = new String[searchResponse.getHits().getHits().length];
            for (int i = 0; i < results.length; i++) {
                Map<String, Object> result = JsonUtil.toMap(searchResponse.getHits().getAt(i).getSourceAsString());
                results[i] = String.valueOf(MapUtil.get(result, path));
            }
            return results;
        }
    }

    @Override
    public QueryHelper getQueryHelper() {
        return this.queryHelper;
    }

    @SneakyThrows({ IOException.class })
    private <T> FacetedSearchResult<T> toFacetedSearchResult(Class<T> clazz, int from, SearchResponse searchResponse) {
        // check something found
        // return an empty object if nothing found
        if (!somethingFound(searchResponse)) {
            T[] resultData = (T[]) Array.newInstance(clazz, 0);
            FacetedSearchResult toReturn = new FacetedSearchResult(from, 0, 0, 0, new String[0], resultData, new HashMap<String, FacetedSearchFacet[]>());
            if (searchResponse != null) {
                toReturn.setQueryDuration(searchResponse.getTook().getMillis());
            }
            return toReturn;
        }

        FacetedSearchResult facetedSearchResult = new FacetedSearchResult();
        fillMultipleDataResult(clazz, searchResponse, facetedSearchResult, from, true); // set data from the query
        parseAggregations(searchResponse, facetedSearchResult, null); // set aggregations
        return facetedSearchResult;
    }

    /**
     * Parse aggregations and set facets to the given non null FacetedSearchResult instance.
     * 
     * @param searchResponse The search response that contains aggregation results.
     * @param facetedSearchResult The instance in which to set facets.
     * @param aggregationQueryManager If not null the data of the FacetedSearchResult will be processed from an aggregation based on the given manager.
     */
    private void parseAggregations(SearchResponse searchResponse, FacetedSearchResult facetedSearchResult, IAggregationQueryManager aggregationQueryManager) {
        if (searchResponse.getAggregations() == null) {
            return;
        }

        List<Aggregation> internalAggregationsList = searchResponse.getAggregations().asList();
        if (internalAggregationsList.size() == 0) {
            return;
        }

        Map<String, FacetedSearchFacet[]> facetMap = Maps.newHashMap();

        for (Aggregation aggregation : internalAggregationsList) {
            if (aggregationQueryManager != null && aggregation.getName().equals(aggregationQueryManager.getQueryAggregation().getName())) {
                aggregationQueryManager.setData(getJsonMapper(), getClassFromTypeFunc(), facetedSearchResult, aggregation);
            } else if (aggregation instanceof InternalTerms) {
                InternalTerms internalTerms = (InternalTerms) aggregation;

                List<FacetedSearchFacet> facets = new ArrayList<>();
                for (int i = 0; i < internalTerms.getBuckets().size(); i++) {
                    Terms.Bucket bucket = (Terms.Bucket)(internalTerms.getBuckets().get(i));
                    facets.add(new FacetedSearchFacet(bucket.getKeyAsString(), bucket.getDocCount()));
                }
                // Find the missing aggregation
                internalAggregationsList.stream()
                        .filter(missingAggregation -> missingAggregation instanceof InternalMissing
                                && missingAggregation.getName().equals("missing_" + internalTerms.getName()))
                        .findAny()
                        // If the missing aggregation is present then add the number of doc with missing value
                        .ifPresent(missingAggregation -> {
                            InternalMissing internalMissingAggregation = (InternalMissing) missingAggregation;
                            if (internalMissingAggregation.getDocCount() > 0) {
                                facets.add(new FacetedSearchFacet(null, internalMissingAggregation.getDocCount()));
                            }
                        });
                facetMap.put(internalTerms.getName(), facets.toArray(new FacetedSearchFacet[facets.size()]));
            } else {
                log.debug("Aggregation is not a facet aggregation (terms) ignore. Name: {} ,Type: {}", aggregation.getName(), aggregation.getClass().getName());
            }
        }
        facetedSearchResult.setFacets(facetMap);
    }

    private Function<String, Class> getClassFromTypeFunc() {
        return s -> getClassFromType(s);
    }

    @Override
    public <T> IESQueryBuilderHelper<T> buildQuery(Class<T> clazz) {
        return new EsQueryBuilderHelper((QueryHelper.QueryBuilderHelper) queryHelper.buildQuery(), clazz);
    }

    @Override
    public <T> IESQueryBuilderHelper<T> buildSearchQuery(Class<T> clazz, String searchQuery) {
        return new EsQueryBuilderHelper((QueryHelper.QueryBuilderHelper) queryHelper.buildQuery(searchQuery), clazz);
    }

    @Override
    public <T> IESQueryBuilderHelper<T> buildSuggestionQuery(Class<T> clazz, String prefixField, String searchQuery) {
        return new EsQueryBuilderHelper((QueryHelper.QueryBuilderHelper) queryHelper.buildQuery(prefixField, searchQuery), clazz);
    }

    /**
     * Extends the QueryBuilderHelper to provide class based indices and types.
     */
    public class EsQueryBuilderHelper<T> extends QueryHelper.QueryBuilderHelper implements IESSearchQueryBuilderHelper {
        private Class<T> clazz;
        private String[] indices;
        private Class<?>[] requestedTypes;
        private String[] esTypes;

        private IESMetaPropertiesSearchContext mpContext;

        protected EsQueryBuilderHelper(QueryHelper.QueryBuilderHelper from, Class<T> clazz) {
            super(from);
            this.clazz = clazz;
            //this.indices = clazz == null ? getAllIndexes() : new String[] { getIndexForType(clazz) };
            this.indices = clazz == null ? getAllIndexes() : getIndexForType(clazz);
            this.requestedTypes = getRequestedTypes(clazz);
            super.types(requestedTypes);
            this.esTypes = getTypes();

            this.mpContext = metaPropertiesSearchHelper.getContext(clazz);
        }

        /**
         * Perform a count request based on the given class.
         *
         * @return The count response.
         */
        public long count() {
            if (log.isTraceEnabled()) {
                log.trace("EsQueryBuilderHelper.count for <{}> query: <{}>", indices, queryBuilder.toString());
            }
            return super.count(indices, esTypes).getHits().getTotalHits();
        }

        @Override
        public QueryBuilder queryBuilder() {
            return super.getQueryBuilder();
        }

        @Override
        public IESSearchQueryBuilderHelper prepareSearch() {
            super.prepareSearch(indices);
            //super.searchRequestBuilder.setTypes(esTypes);
            super.searchRequestBuilder.setQuery(queryBuilder);
            return this;
        }

        @Override
        public T find() {
            GetMultipleDataResult<T> result = search(0, 1);
            if (result.getData() == null || result.getData().length == 0) {
                return null;
            }
            return result.getData()[0];
        }

        public GetMultipleDataResult<T> search(int from, int size) {
            if (log.isTraceEnabled()) {
                log.trace("EsQueryBuilderHelper.search for <{}> from {} size {} query: <{}>", indices, from, size, queryBuilder.toString());
            }
            return toGetMultipleDataResult(clazz, super.execute(from, size), from);
        }

        @Override
        public FacetedSearchResult facetedSearch(int from, int size) {
            List<IFacetBuilderHelper> facetBuilderHelpers = mpContext.getFacetBuilderHelpers();

            super.facets(facetBuilderHelpers);

            if (size > 10000) {
               size = 10000;
            }
            FacetedSearchResult  facetedSearchResult = toFacetedSearchResult(clazz, from, super.execute(from, size));

            // Convert metaProperties name
            mpContext.postProcess(facetedSearchResult);

            return facetedSearchResult;
        }

        @Override
        public FacetedSearchResult facetedSearch(IAggregationQueryManager aggregationQueryManager) {
            List<IFacetBuilderHelper> facetBuilderHelpers = mpContext.getFacetBuilderHelpers();

            //searchRequestBuilder.setSearchType(SearchType.COUNT);
            searchRequestBuilder.addAggregation(aggregationQueryManager.getQueryAggregation());

            super.facets(facetBuilderHelpers);

            SearchResponse searchResponse = super.execute(0, 0);

            FacetedSearchResult facetedSearchResult = new FacetedSearchResult();
            parseAggregations(searchResponse, facetedSearchResult, aggregationQueryManager);

            // Convert metaProperties name
            mpContext.postProcess(facetedSearchResult);

            return facetedSearchResult;
        }

        @Override
        public EsQueryBuilderHelper setScriptFunction(String functionScore) {
            super.scriptFunction(functionScore);
            return this;
        }

        @Override
        public EsQueryBuilderHelper setFilters(QueryBuilder... customFilters) {
            super.filters(customFilters);
            return this;
        }

        @Override
        public EsQueryBuilderHelper setFilters(Map filters, Map filterStrategies, QueryBuilder... customFilters) {
            // MetaProperties Name Conversion
            mpContext.preProcess(filters);

            // Add MetaProperties filters
            customFilters = ObjectArrays.concat(customFilters, mpContext.getFilterBuilders(filters),QueryBuilder.class);

            super.filters(filters, filterStrategies, customFilters);
            return this;
        }

        @Override
        public EsQueryBuilderHelper setFilters(Map filters, QueryBuilder... customFilters) {
            // MetaProperties Name Conversion
            mpContext.preProcess(filters);

            // Add MetaProperties filters
            customFilters = ObjectArrays.concat(customFilters, mpContext.getFilterBuilders(filters),QueryBuilder.class);

            super.filters(filters, customFilters);
            return this;
        }

        @Override
        public EsQueryBuilderHelper alterQueryBuilder(QueryBuilderAdapter queryBuilderAdapter) {
            super.alterQuery(queryBuilderAdapter);
            return this;
        }

        @Override
        public EsQueryBuilderHelper setFieldSort(String fieldName, String fieldType, boolean desc) {
            super.fieldSort(fieldName, fieldType, desc);
            return this;
        }

        @Override
        public EsQueryBuilderHelper setFetchContext(String fetchContext) {
            super.fetchContext(fetchContext);
            return this;
        }

        @Override
        public IESSearchQueryBuilderHelper setFetchContext(String fetchContext, TopHitsAggregationBuilder topHitsBuilder) {
            super.fetchContext(fetchContext, topHitsBuilder);
            return this;
        }

        @Override
        public EsQueryBuilderHelper alterSearchRequestBuilder(ISearchBuilderAdapter adapter) {
            super.alterSearchRequest(adapter);
            return this;
        }
    }
}
