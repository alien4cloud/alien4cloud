package alien4cloud.component;

import static alien4cloud.dao.FilterUtil.kvCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import javax.annotation.Resource;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.dao.IAggregationQueryManager;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.utils.CollectionUtils;
import alien4cloud.utils.VersionUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Primary
public class CSARRepositorySearchService implements ICSARRepositorySearchService, IElementSearchService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO searchDAO;

    @Override
    public Csar getArchive(String id) {
        return searchDAO.findById(Csar.class, id);
    }

    /**
     * Find an element based on it's type and id.
     *
     * @param elementType The element type.
     * @param elementId The element id.
     * @return Return the matching
     */
    public <T extends IndexedToscaElement> T[] findByElementId(Class<T> elementType, String elementId) {
        return searchDAO.buildQuery(elementType).setFilters(singleKeyFilter("elementId", elementId)).prepareSearch().search(0, Integer.MAX_VALUE).getData();
    }

    @Override
    public boolean isElementExistInDependencies(@NonNull Class<? extends IndexedToscaElement> elementClass, @NonNull String elementId,
            Collection<CSARDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return false;
        }
        // The query match element id of all defined dependencies' version from defined dependencies' archive name
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (CSARDependency dependency : dependencies) {
            QueryBuilder idQueryBuilder = QueryBuilders.idsQuery().addIds(elementId + ":" + dependency.getVersion());
            QueryBuilder matchArchiveNameQueryBuilder = QueryBuilders.termQuery("archiveName", dependency.getName());
            boolQueryBuilder.should(QueryBuilders.boolQuery().must(idQueryBuilder).must(matchArchiveNameQueryBuilder));
        }
        return searchDAO.count(elementClass, boolQueryBuilder) > 0;
    }

    private <T extends IndexedToscaElement> T getLatestVersionOfElement(Class<T> elementClass, QueryBuilder queryBuilder) {
        List<T> elements = searchDAO.customFindAll(elementClass, queryBuilder);
        if (elements != null && !elements.isEmpty()) {
            Collections.sort(elements, new Comparator<T>() {
                @Override
                public int compare(T left, T right) {
                    return VersionUtil.parseVersion(left.getArchiveVersion()).compareTo(VersionUtil.parseVersion(right.getArchiveVersion()));
                }
            });
            return elements.get(elements.size() - 1);
        } else {
            return null;
        }
    }

    @Override
    public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return null;
        }
        // The query match element id of all defined dependencies' version from defined dependencies' archive name
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (CSARDependency dependency : dependencies) {
            QueryBuilder idQueryBuilder = QueryBuilders.idsQuery().addIds(elementId + ":" + dependency.getVersion());
            QueryBuilder matchArchiveNameQueryBuilder = QueryBuilders.termQuery("archiveName", dependency.getName());
            boolQueryBuilder.should(QueryBuilders.boolQuery().must(idQueryBuilder).must(matchArchiveNameQueryBuilder));
        }
        return getLatestVersionOfElement(elementClass, boolQueryBuilder);
    }

    public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, QueryBuilder query, Collection<CSARDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return null;
        }
        // The query match element id of all defined dependencies' version from defined dependencies' archive name
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (CSARDependency dependency : dependencies) {
            QueryBuilder matchArchiveNameQueryBuilder = QueryBuilders.termQuery("archiveName", dependency.getName());
            QueryBuilder matchArchiveVersionQueryBuilder = QueryBuilders.termQuery("archiveVersion", dependency.getVersion());
            boolQueryBuilder.should(QueryBuilders.boolQuery().must(query).must(matchArchiveNameQueryBuilder).must(matchArchiveVersionQueryBuilder));
        }
        return getLatestVersionOfElement(elementClass, boolQueryBuilder);
    }

    @Override
    public <T extends IndexedToscaElement> T getRequiredElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies)
            throws NotFoundException {
        T element = getElementInDependencies(elementClass, elementId, dependencies);
        if (element == null) {
            throw new NotFoundException(
                    "Element elementId: <" + elementId + "> of type <" + elementClass.getSimpleName() + "> cannot be found in dependencies " + dependencies);
        }
        return element;
    }

    @Override
    public <T extends IndexedToscaElement> T getParentOfElement(Class<T> elementClass, T indexedToscaElement, String parentElementId) {
        Csar csar = searchDAO.findById(Csar.class, indexedToscaElement.getArchiveName() + ":" + indexedToscaElement.getArchiveVersion());
        Set<CSARDependency> dependencies = Sets.newHashSet(new CSARDependency(csar.getName(), csar.getVersion()));
        dependencies = CollectionUtils.merge(csar.getDependencies(), dependencies);
        return getRequiredElementInDependencies(elementClass, parentElementId, CollectionUtils.merge(csar.getDependencies(), dependencies));
    }

    @Override
    public FacetedSearchResult search(Class<? extends IndexedToscaElement> clazz, String query, Integer size, Map<String, String[]> filters) {
        TopHitsBuilder topHitAggregation = AggregationBuilders.topHits("highest_version").setSize(1)
                .addSort(new FieldSortBuilder("nestedVersion.majorVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.minorVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.incrementalVersion").order(SortOrder.DESC))
                .addSort(new FieldSortBuilder("nestedVersion.qualifier").order(SortOrder.DESC).missing("_first"));

        AggregationBuilder aggregation = AggregationBuilders.terms("query_aggregation").field("elementId").size(size).subAggregation(topHitAggregation);

        FacetedSearchResult<? extends IndexedToscaElement> searchResult = searchDAO.buildSearchQuery(clazz, query).setFilters(filters).prepareSearch()
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
                            result.setData(new IndexedToscaElement[0]);
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

                        result.setData(resultData.toArray(new IndexedToscaElement[resultData.size()]));
                        result.setTypes(resultTypes.toArray(new String[resultTypes.size()]));
                    }
                });

        return searchResult;
    }

}