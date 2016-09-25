package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.VersionUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Primary
public class ToscaTypeSearchService extends AbstractToscaIndexSearchService<AbstractToscaType> implements IToscaTypeSearchService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO searchDAO;

    @Override
    public Csar getArchive(String archiveName, String archiveVersion) {
        return searchDAO.buildQuery(Csar.class).prepareSearch().setFilters(fromKeyValueCouples("name", archiveName, "version", archiveVersion)).find();
    }

    @Override
    public boolean hasTypes(String archiveName, String archiveVersion) {
        return searchDAO.buildQuery(AbstractToscaType.class).setFilters(fromKeyValueCouples("archiveName", archiveName, "archiveVersion", archiveVersion))
                .count() > 0;
    }

    @Override
    public AbstractToscaType[] getArchiveTypes(String archiveName, String archiveVersion) {
        return searchDAO.buildQuery(AbstractToscaType.class).setFilters(fromKeyValueCouples("archiveName", archiveName, "archiveVersion", archiveVersion))
                .prepareSearch().search(0, Integer.MAX_VALUE).getData();
    }

    @Override
    public <T extends AbstractToscaType> T find(Class<T> elementType, String elementId, String version) {
        return searchDAO.buildQuery(elementType).setFilters(fromKeyValueCouples("rawElementId", elementId, "archiveVersion", version)).prepareSearch().find();
    }

    @Override
    public <T extends AbstractToscaType> T findMostRecent(Class<T> elementType, String elementId) {
        return searchDAO.buildQuery(elementType).setFilters(fromKeyValueCouples("rawElementId", elementId)).prepareSearch()
                .alterSearchRequestBuilder(
                        searchRequestBuilder -> searchRequestBuilder.addSort(new FieldSortBuilder("nestedVersion.majorVersion").order(SortOrder.DESC))
                                .addSort(new FieldSortBuilder("nestedVersion.minorVersion").order(SortOrder.DESC))
                                .addSort(new FieldSortBuilder("nestedVersion.incrementalVersion").order(SortOrder.DESC))
                                .addSort(new FieldSortBuilder("nestedVersion.qualifier").order(SortOrder.DESC).missing("_first")))
                .find();
    }

    @Override
    public <T extends AbstractToscaType> T[] findAll(Class<T> elementType, String elementId) {
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
    public boolean isElementExistInDependencies(@NonNull Class<? extends AbstractToscaType> elementClass, @NonNull String elementId,
            Set<CSARDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return false;
        }
        return searchDAO.count(elementClass, getDependencyQuery(dependencies, "rawElementId", elementId)) > 0;
    }

    private <T extends AbstractToscaType> T getLatestVersionOfElement(Class<T> elementClass, QueryBuilder queryBuilder) {
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
    public <T extends AbstractToscaType> T getElementInDependencies(Class<T> elementClass, Set<CSARDependency> dependencies, String... keyValues) {
        if (dependencies == null || dependencies.isEmpty()) {
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = getDependencyQuery(dependencies, keyValues);
        return getLatestVersionOfElement(elementClass, boolQueryBuilder);
    }

    @Override
    public <T extends AbstractToscaType> T getElementInDependencies(Class<T> elementClass, String elementId, Set<CSARDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = getDependencyQuery(dependencies, "rawElementId", elementId);
        return getLatestVersionOfElement(elementClass, boolQueryBuilder);
    }

    @Override
    public <T extends AbstractToscaType> T getRequiredElementInDependencies(Class<T> elementClass, String elementId, Set<CSARDependency> dependencies)
            throws NotFoundException {
        T element = getElementInDependencies(elementClass, elementId, dependencies);
        if (element == null) {
            throw new NotFoundException(
                    "Element elementId: <" + elementId + "> of type <" + elementClass.getSimpleName() + "> cannot be found in dependencies " + dependencies);
        }
        return element;
    }

    // we need to override for aspect purpose
    @Override
    public FacetedSearchResult search(Class<? extends AbstractToscaType> clazz, String query, Integer size, Map<String, String[]> filters) {
        return super.search(clazz, query, size, filters);
    }

    @Override
    protected AbstractToscaType[] getArray(int size) {
        return new AbstractToscaType[size];
    }

    @Override
    protected String getAggregationField() {
        return "elementId";
    }
}