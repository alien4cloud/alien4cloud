package alien4cloud.component;

import static alien4cloud.dao.FilterUtil.kvCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import alien4cloud.dao.IAggregationQueryManager;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedToscaElement;
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
    public Csar getArchive(String archiveName, String archiveVersion) {
        return searchDAO.buildQuery(Csar.class).prepareSearch().setFilters(kvCouples("name", archiveName, "version", archiveVersion)).find();
    }

    /**
     * Find an element based on it's type and id.
     *
     * @param elementType The element type.
     * @param elementId The element id.
     * @return Return the matching
     */
    public <T extends IndexedToscaElement> T[] findByElementId(Class<T> elementType, String elementId) {
        return searchDAO.buildQuery(elementType).setFilters(singleKeyFilter("rawElementId", elementId)).prepareSearch().search(0, Integer.MAX_VALUE).getData();
    }

    /**
     * Build an elasticsearch query to get data tosca elements based on a set of dependencies.
     *
     * @param dependencies The set of dependencies.
     * @param keyValueFilters List of key1, value1, key2, value2 to add term filters to the query for each dependency.
     * @return
     */
    private BoolQueryBuilder getDependencyQuery(Set<CSARDependency> dependencies, String... keyValueFilters) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (CSARDependency dependency : dependencies) {
            BoolQueryBuilder dependencyQuery = QueryBuilders.boolQuery();
            dependencyQuery.must(QueryBuilders.termQuery("archiveName", dependency.getName()))
                    .must(QueryBuilders.termQuery("archiveVersion", dependency.getVersion()));
            if (keyValueFilters != null) {
                for (int i = 0; i < keyValueFilters.length; i += 2) {
                    dependencyQuery.must(QueryBuilders.termQuery(keyValueFilters[i], keyValueFilters[i + 1]));
                }
            }
            boolQueryBuilder.should(dependencyQuery);
        }
        return boolQueryBuilder;
    }

    @Override
    public boolean isElementExistInDependencies(@NonNull Class<? extends IndexedToscaElement> elementClass, @NonNull String elementId,
            Set<CSARDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return false;
        }
        return searchDAO.count(elementClass, getDependencyQuery(dependencies, "rawElementId", elementId)) > 0;
    }

    private <T extends IndexedToscaElement> T getLatestVersionOfElement(Class<T> elementClass, QueryBuilder queryBuilder) {
        List<T> elements = searchDAO.customFindAll(elementClass, queryBuilder);
        if (elements != null && !elements.isEmpty()) {
            Collections.sort(elements,
                    (left, right) -> VersionUtil.parseVersion(left.getArchiveVersion()).compareTo(VersionUtil.parseVersion(right.getArchiveVersion())));
            return elements.get(elements.size() - 1);
        } else {
            return null;
        }
    }

    @Override
    public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, Set<CSARDependency> dependencies, String... keyValues) {
        if (dependencies == null || dependencies.isEmpty()) {
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = getDependencyQuery(dependencies, keyValues);
        return getLatestVersionOfElement(elementClass, boolQueryBuilder);
    }

    @Override
    public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Set<CSARDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = getDependencyQuery(dependencies, "rawElementId", elementId);
        return getLatestVersionOfElement(elementClass, boolQueryBuilder);
    }

    @Override
    public <T extends IndexedToscaElement> T getRequiredElementInDependencies(Class<T> elementClass, String elementId, Set<CSARDependency> dependencies)
            throws NotFoundException {
        T element = getElementInDependencies(elementClass, elementId, dependencies);
        if (element == null) {
            throw new NotFoundException(
                    "Element elementId: <" + elementId + "> of type <" + elementClass.getSimpleName() + "> cannot be found in dependencies " + dependencies);
        }
        return element;
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