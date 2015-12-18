package alien4cloud.component;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import lombok.NonNull;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.utils.CollectionUtils;
import alien4cloud.utils.VersionUtil;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Component
public class CSARRepositorySearchService implements ICSARRepositorySearchService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO searchDAO;

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
    public FacetedSearchResult search(Class<? extends IndexedToscaElement> classNameToQuery, String query, Integer from, Integer size,
            Map<String, String[]> filters, boolean queryAllVersions) {
        if (!queryAllVersions) {
            if (filters == null) {
                filters = Maps.newHashMap();
            }
            filters.put("highestVersion", new String[] { "true" });
        }
        FacetedSearchResult searchResult = searchDAO.facetedSearch(classNameToQuery, query, filters, FetchContext.SUMMARY, from, size);
        return searchResult;
    }

}
