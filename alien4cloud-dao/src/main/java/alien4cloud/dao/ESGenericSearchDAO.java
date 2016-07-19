package alien4cloud.dao;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.SneakyThrows;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.elasticsearch.mapping.FilterValuesStrategy;
import org.elasticsearch.mapping.MappingBuilder;
import org.elasticsearch.mapping.QueryBuilderAdapter;
import org.elasticsearch.mapping.QueryHelper;
import org.elasticsearch.mapping.QueryHelper.SearchQueryHelperBuilder;
import org.elasticsearch.mapping.SourceFetchContext;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import alien4cloud.dao.model.FacetedSearchFacet;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.ElasticSearchUtil;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;

/**
 * Elastic search dao that manages search operations.
 *
 * @author luc boutier
 */
public abstract class ESGenericSearchDAO extends ESGenericIdDAO implements IGenericSearchDAO {
    // private static final String SCORE_SCRIPT = "_score * ((doc.containsKey('alienScore') && !doc['alienScore'].empty) ? doc['alienScore'].value : 1)";
    @Resource
    private ElasticSearchClient esClient;
    @Resource
    private QueryHelper queryHelper;

    @Override
    public <T> long count(Class<T> clazz, QueryBuilder query) {
        String indexName = getIndexForType(clazz);
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        CountRequestBuilder countRequestBuilder = getClient().prepareCount(indexName).setTypes(typeName);
        if (query != null) {
            countRequestBuilder.setQuery(query);
        }
        return countRequestBuilder.execute().actionGet().getCount();
    }

    @Override
    public <T> long count(Class<T> clazz, String searchText, Map<String, String[]> filters) {
        String[] searchIndexes = clazz == null ? getAllIndexes() : new String[]{getIndexForType(clazz)};
        Class<?>[] requestedTypes = getRequestedTypes(clazz);

        return this.queryHelper.buildCountQuery(searchIndexes, searchText).types(requestedTypes).filters(filters).count().getCount();
    }

    @Override
    public void delete(Class<?> clazz, QueryBuilder query) {
        String indexName = getIndexForType(clazz);
        String typeName = MappingBuilder.indexTypeFromClass(clazz);

        // get all elements and then use a bulk delete to remove data.
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexName).setTypes(getTypesFromClass(clazz)).setQuery(query).setNoFields().setFetchSource(false);
        searchRequestBuilder.setFrom(0).setSize(1000);
        SearchResponse response = searchRequestBuilder.execute().actionGet();

        while (somethingFound(response)) {
            BulkRequestBuilder bulkRequestBuilder = getClient().prepareBulk().setRefresh(true);

            for (int i = 0; i < response.getHits().hits().length; i++) {
                String id = response.getHits().hits()[i].getId();
                bulkRequestBuilder.add(getClient().prepareDelete(indexName, typeName, id));
            }

            bulkRequestBuilder.execute().actionGet();

            if (response.getHits().totalHits() == response.getHits().hits().length) {
                response = null;
            } else {
                response = searchRequestBuilder.execute().actionGet();
            }
        }
    }

    @SneakyThrows({IOException.class})
    private <T> List<T> doCustomFind(Class<T> clazz, QueryBuilder query, SortBuilder sortBuilder, int size) {
        String indexName = getIndexForType(clazz);
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexName).setTypes(getTypesFromClass(clazz)).setSize(size);
        if (query != null) {
            searchRequestBuilder.setQuery(query);
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
                String hit = response.getHits().getAt(i).sourceAsString();
                hits.add((T) getJsonMapper().readValue(hit, getClassFromType(response.getHits().getAt(i).getType())));
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
        List<T> results = doCustomFind(clazz, query, sortBuilder, 1);
        if (results == null || results.isEmpty()) {
            return null;
        } else {
            return results.iterator().next();
        }
    }

    @Override
    public <T> List<T> customFindAll(Class<T> clazz, QueryBuilder query) {
        return customFindAll(clazz, query, null);
    }

    @Override
    public <T> List<T> customFindAll(Class<T> clazz, QueryBuilder query, SortBuilder sortBuilder) {
        return doCustomFind(clazz, query, sortBuilder, Integer.MAX_VALUE);
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
    public GetMultipleDataResult<Object> search(SearchQueryHelperBuilder queryHelperBuilder, int from, int maxElements) {
        return toGetMultipleDataResult(Object.class, queryHelperBuilder.search(from, maxElements), from);
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
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter,
                                               String fetchContext, int from, int maxElements) {
        return search(clazz, searchText, filters, customFilter, fetchContext, from, maxElements, null, false);
    }

    @Override
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter,
                                               String fetchContext, int from, int maxElements, String fieldSort, boolean sortOrder) {
        SearchResponse searchResponse = doSearch(clazz, searchText, filters, customFilter, fetchContext, from, maxElements, false, fieldSort, sortOrder);
        return toGetMultipleDataResult(clazz, searchResponse, from);
    }

    @Override
    public GetMultipleDataResult<Object> search(String[] searchIndices, Class<?>[] classes, String searchText, Map<String, String[]> filters,
                                                String fetchContext, int from, int maxElements) {
        return search(searchIndices, classes, searchText, filters, null, fetchContext, from, maxElements);
    }

    @Override
    public GetMultipleDataResult<Object> search(String[] searchIndices, Class<?>[] classes, String searchText, Map<String, String[]> filters,
                                                FilterBuilder customFilter, String fetchContext, int from, int maxElements) {
        SearchResponse searchResponse = queryHelper.buildSearchQuery(searchIndices, searchText).fetchContext(fetchContext).filters(filters)
                .customFilter(customFilter).types(classes).search(from, maxElements);
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
    public <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter,
                                                 String fetchContext, int from, int maxElements) {
        return facetedSearch(clazz, searchText, filters, customFilter, fetchContext, from, maxElements, null, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    @SneakyThrows({IOException.class})
    public <T> FacetedSearchResult facetedSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter,
                                                 String fetchContext, int from, int maxElements, String fieldSort, boolean sortOrder) {
        SearchResponse searchResponse = doSearch(clazz, searchText, filters, customFilter, fetchContext, from, maxElements, true, fieldSort, sortOrder);

        // check something found
        // return an empty object if nothing found
        if (!somethingFound(searchResponse)) {
            T[] resultData = (T[]) Array.newInstance(clazz, 0);
            FacetedSearchResult toReturn = new FacetedSearchResult(from, 0, 0, 0, new String[0], resultData, new HashMap<String, FacetedSearchFacet[]>());
            if (searchResponse != null) {
                toReturn.setQueryDuration(searchResponse.getTookInMillis());
            }
            return toReturn;
        }

        FacetedSearchResult finalResponse = new FacetedSearchResult();

        fillMultipleDataResult(clazz, searchResponse, finalResponse, from, true);

        finalResponse.setFacets(parseFacets(searchResponse.getFacets()));

        return finalResponse;
    }

    @Override
    public GetMultipleDataResult<Object> suggestSearch(String[] searchIndices, Class<?>[] requestedTypes, String suggestFieldPath, String searchPrefix,
                                                       String fetchContext, int from, int maxElements) {
        SearchResponse searchResponse = queryHelper.buildSearchSuggestQuery(searchIndices, searchPrefix, suggestFieldPath).types(requestedTypes)
                .fetchContext(fetchContext).search(from, maxElements);

        return toGetMultipleDataResult(Object.class, searchResponse, from);
    }

    @Override
    public <T> GetMultipleDataResult<T> search(Class<T> clazz, String searchText, Map<String, String[]> filters,
                                               Map<String, FilterValuesStrategy> filterStrategies, int maxElements) {
        String[] searchIndices = clazz == null ? getAllIndexes() : new String[]{getIndexForType(clazz)};
        Class<?>[] requestedTypes = getRequestedTypes(clazz);

        SearchResponse searchResponse = queryHelper.buildSearchQuery(searchIndices).types(requestedTypes).filters(filters).filterStrategies(filterStrategies)
                .search(0, maxElements);

        return toGetMultipleDataResult(clazz, searchResponse, 0);
    }

    /**
     * Convert a SearchResponse into a {@link GetMultipleDataResult} including json deserialization.
     *
     * @param searchResponse The actual search response from elastic-search.
     * @param from           The start index of the search request.
     * @return A {@link GetMultipleDataResult} instance that contains de-serialized data.
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows({IOException.class})
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
     * @param clazz          The type of objects to de-serialize.
     * @return A list of instances that contains de-serialized data.
     */
    @SneakyThrows({IOException.class})
    public <T> List<T> toGetListOfData(SearchResponse searchResponse, Class<T> clazz) {
        // return null if no data has been found in elastic search.
        if (!somethingFound(searchResponse)) {
            return null;
        }

        List<T> result = new ArrayList<>();

        for (int i = 0; i < searchResponse.getHits().getHits().length; i++) {
            result.add(getJsonMapper().readValue(searchResponse.getHits().getAt(i).getSourceAsString(), clazz));
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
            finalResponse.setQueryDuration(searchResponse.getTookInMillis());
        }

        String[] resultTypes = new String[searchResponse.getHits().getHits().length];

        T[] resultData = (T[]) Array.newInstance(clazz, resultTypes.length);
        for (int i = 0; i < resultTypes.length; i++) {
            resultTypes[i] = searchResponse.getHits().getAt(i).getType();
            resultData[i] = (T) getJsonMapper().readValue(searchResponse.getHits().getAt(i).getSourceAsString(), getClassFromType(resultTypes[i]));
        }
        finalResponse.setData(resultData);

        finalResponse.setTypes(resultTypes);
    }

    private <T> SearchResponse doSearch(Class<T> clazz, String searchText, Map<String, String[]> filters, FilterBuilder customFilter, String fetchContext,
                                        int from, int maxElements, boolean enableFacets, String fieldSort, boolean sortOrder) {
        String[] searchIndexes = clazz == null ? getAllIndexes() : new String[]{getIndexForType(clazz)};
        Class<?>[] requestedTypes = getRequestedTypes(clazz);
        String[] esTypes = getTypesStrings(requestedTypes);

        SearchQueryHelperBuilder query = this.queryHelper.buildSearchQuery(searchIndexes, searchText).types(requestedTypes).fetchContext(fetchContext)
                .filters(filters).customFilter(customFilter).facets(enableFacets);
        if (fieldSort != null) {
            query.fieldSort(fieldSort, sortOrder);
        }

        SearchRequestBuilder searchRequestBuilder = query.generate(from, maxElements, new QueryBuilderAdapter() {
            @Override
            public QueryBuilder adapt(QueryBuilder queryBuilder) {
                return QueryBuilders.functionScoreQuery(queryBuilder).scoreMode("multiply").boostMode(CombineFunction.MULT)
                        .add(ScoreFunctionBuilders.fieldValueFactorFunction("alienScore").missing(1));
            }
        });
        searchRequestBuilder.setTypes(esTypes);
        return searchRequestBuilder.execute().actionGet();
    }

    private boolean somethingFound(final SearchResponse searchResponse) {
        if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().getHits() == null
                || searchResponse.getHits().getHits().length == 0) {
            return false;
        }
        return true;
    }

    private Map<String, FacetedSearchFacet[]> parseFacets(Facets facets) {
        if (facets == null || facets.getFacets().size() == 0) {
            return null;
        }

        Map<String, FacetedSearchFacet[]> toReturnMap = new HashMap<>();
        List<FacetedSearchFacet> fsf = null;

        for (Facet facet : facets) {
            fsf = null;
            if (facet instanceof TermsFacet) {
                TermsFacet termFacet = (TermsFacet) facet;
                fsf = Lists.newArrayList();
                for (TermsFacet.Entry entry : termFacet.getEntries()) {
                    fsf.add(new FacetedSearchFacet(entry.getTerm().string(), entry.getCount()));
                }

                // add the missing count as a facet with null value.
                // This will help filtering on null values of a facet
                // TODO: wrap the Map<String, FacetedSearchFacet[]> with an object adding a missingCout field
                if (termFacet.getMissingCount() > 0) {
                    fsf.add(new FacetedSearchFacet(null, termFacet.getMissingCount()));
                }
            }
            toReturnMap.put(facet.getName(), fsf.toArray(new FacetedSearchFacet[fsf.size()]));
        }

        return toReturnMap;
    }

    @Override
    public <T> List<T> findByIdsWithContext(Class<T> clazz, String fetchContext, String... ids) {

        // get the fetch context for the given type and apply it to the search
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();
        SourceFetchContext sourceFetchContext = getMappingBuilder().getFetchSource(clazz.getName(), fetchContext);
        if (sourceFetchContext != null) {
            includes.addAll(sourceFetchContext.getIncludes());
            excludes.addAll(sourceFetchContext.getExcludes());
        } else {
            getLog().warn("Unable to find fetch context <" + fetchContext + "> for class <" + clazz.getName() + ">. It will be ignored.");
        }

        String[] inc = includes.isEmpty() ? null : includes.toArray(new String[includes.size()]);
        String[] exc = excludes.isEmpty() ? null : excludes.toArray(new String[excludes.size()]);

        // TODO: correctly manage "from" and "size"
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(getIndexForType(clazz))
                .setQuery(QueryBuilders.idsQuery(MappingBuilder.indexTypeFromClass(clazz)).ids(ids)).setFetchSource(inc, exc).setSize(20);

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

    @SneakyThrows({IOException.class})
    private String[] doSelectPath(String index, String[] types, QueryBuilder queryBuilder, SortOrder sortOrder, String path, int from, int size) {
        SearchRequestBuilder searchRequestBuilder = esClient.getClient().prepareSearch(index);
        searchRequestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH).setQuery(queryBuilder).setSize(size).setFrom(from);
        searchRequestBuilder.setFetchSource(path, null);
        searchRequestBuilder.setTypes(types);
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

}
