package org.alien4cloud.tosca.catalog.index;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.VersionUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;

@Slf4j
@Component
@Primary
public class ToscaTypeSearchService extends AbstractToscaIndexSearchService<AbstractToscaType> implements IToscaTypeSearchService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO searchDAO;


    @Value("${components.search.boost.name_query_similitude:100}")
    private Integer nameQuerySimilitudeBoost;

    @Override
    public Csar getArchive(String archiveName, String archiveVersion) {
        return searchDAO.findById(Csar.class, Csar.createId(archiveName, archiveVersion));
    }

    @Override
    public boolean hasTypes(String archiveName, String archiveVersion) {
        return searchDAO.buildQuery(AbstractToscaType.class).setFilters(fromKeyValueCouples("archiveName", archiveName, "archiveVersion", archiveVersion))
                .count() > 0;
    }

    @Override
    public AbstractToscaType[] getArchiveTypes(String archiveName, String archiveVersion) {
        return searchDAO.buildQuery(AbstractToscaType.class).setFilters(fromKeyValueCouples("archiveName", archiveName, "archiveVersion", archiveVersion))
                .prepareSearch().search(0, 10000).getData();
    }

    @Override
    public <T extends AbstractToscaType> T find(Class<T> elementType, String elementId, String version) {
 	 return searchDAO.buildQuery(elementType).setFilters(fromKeyValueCouples("elementId.rawElementId", elementId, "archiveVersion", version)).prepareSearch().find();
    }

    public <T extends AbstractToscaType> T findByIdOrFail(Class<T> elementType, String toscaTypeId) {
        T type = searchDAO.findById(elementType, toscaTypeId);
        if (type == null) {
            throw new NotFoundException(String.format("[%s] [%s] does not exists.", elementType.getSimpleName(), toscaTypeId));
        }
        return type;
    }

    @Override
    public <T extends AbstractToscaType> T findOrFail(Class<T> elementType, String elementId, String version) {
        T type = find(elementType, elementId, version);
        if (type == null) {
            throw new NotFoundException(String.format("[%s] [%s] does not exists with version [%s].", elementType.getSimpleName(), elementId, version));
        }
        return type;
    }

    @Override
    public <T extends AbstractToscaType> T findMostRecent(Class<T> elementType, String elementId) {
        return searchDAO.buildQuery(elementType).setFilters(fromKeyValueCouples("elementId.rawElementId", elementId)).prepareSearch()
                .alterSearchRequestBuilder(
                        searchRequestBuilder -> searchRequestBuilder.addSort(new FieldSortBuilder("nestedVersion.majorVersion").order(SortOrder.DESC))
                                .addSort(new FieldSortBuilder("nestedVersion.minorVersion").order(SortOrder.DESC))
                                .addSort(new FieldSortBuilder("nestedVersion.incrementalVersion").order(SortOrder.DESC))
                                .addSort(new FieldSortBuilder("nestedVersion.qualifier").order(SortOrder.DESC).missing("_first")))
                .find();
    }

    @Override
    public <T extends AbstractToscaType> T[] findAll(Class<T> elementType, String elementId) {
        return searchDAO.buildQuery(elementType).setFilters(singleKeyFilter("elementId.rawElementId", elementId)).prepareSearch().search(0, 10000).getData();
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
        return searchDAO.count(elementClass, getDependencyQuery(dependencies, "elementId.rawElementId", elementId)) > 0;
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
        if (dependencies == null || dependencies.isEmpty() || (elementId == null)) {
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = getDependencyQuery(dependencies, "elementId.rawElementId", elementId);
        return getLatestVersionOfElement(elementClass, boolQueryBuilder);
    }

    @Override
    public <T extends AbstractToscaType> T getRequiredElementInDependencies(Class<T> elementClass, String elementId, Set<CSARDependency> dependencies)
            throws NotFoundException {
        T element = getElementInDependencies(elementClass, elementId, dependencies);
        if (element == null) {
            throw new NotFoundException(
                    "Element elementId: [" + elementId + "] of type [" + elementClass.getSimpleName() + "] cannot be found in dependencies " + dependencies);
        }
        return element;
    }

    // we need to override for aspect purpose
    @Override
    public FacetedSearchResult search(Class<? extends AbstractToscaType> clazz, String query, Integer size, Map<String, String[]> filters) {
        FacetedSearchResult result = super.search(clazz, query, size, filters);
        reorderIfNodeType(clazz, query, result);
        return result;
    }

    private void reorderIfNodeType(Class<? extends AbstractToscaType> clazz, String query, FacetedSearchResult result) {
        if (NodeType.class.isAssignableFrom(clazz)) {
            Arrays.sort(result.getData(), Comparator.comparingLong(value -> {
                NodeType nodeType = (NodeType)value;
                String normalizedElementId = nodeType.getElementId().toLowerCase();
                String simplifiedNodeType = normalizedElementId.substring(normalizedElementId.lastIndexOf('.') + 1);
                if (query != null && simplifiedNodeType.equals(query.toLowerCase())) {
                    return nameQuerySimilitudeBoost * 2 * nodeType.getAlienScore();
                } else if (query != null && normalizedElementId.contains(query.toLowerCase())) {
                    return nameQuerySimilitudeBoost * nodeType.getAlienScore();
                } else {
                    return nodeType.getAlienScore();
                }
            }).reversed());
        }
    }

    @Override
    protected AbstractToscaType[] getArray(int size) {
        return new AbstractToscaType[size];
    }

    @Override
    protected String getAggregationField() {
        return "elementId.rawElementId";
    }
}
