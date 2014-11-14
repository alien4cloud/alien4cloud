package alien4cloud.tosca.container.services.csar.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.component.model.IndexedModelUtils;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.container.services.csar.ICSARRepositoryIndexerService;
import alien4cloud.utils.VersionUtil;

import com.google.common.collect.Sets;

@Component
public class CSARRepositoryIndexerService implements ICSARRepositoryIndexerService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ElasticSearchClient elasticSearchClient;

    private void refreshIndexForSearching() {
        elasticSearchClient.getClient().admin().indices().prepareRefresh(ElasticSearchDAO.TOSCA_ELEMENT_INDEX).execute().actionGet();
    }

    @Override
    public void indexElements(String archiveName, String archiveVersion, Map<String, IndexedToscaElement> archiveElements) {
        for (IndexedToscaElement element : archiveElements.values()) {
            saveAndUpdateHighestVersion(element);
        }
    }

    @Override
    public void indexInheritableElements(String archiveName, String archiveVersion, Map<String, ? extends IndexedInheritableToscaElement> archiveElements,
            Collection<CSARDependency> dependencies) {
        List<IndexedInheritableToscaElement> orderedElements = IndexedModelUtils.orderForIndex(archiveElements);
        for (IndexedInheritableToscaElement element : orderedElements) {
            indexInheritableElement(archiveName, archiveVersion, element, dependencies);
        }
    }

    @Override
    public void indexInheritableElement(String archiveName, String archiveVersion, IndexedInheritableToscaElement element,
            Collection<CSARDependency> dependencies) {
        if (element.getDerivedFrom() != null) {
            Class<? extends IndexedInheritableToscaElement> indexedType = element.getClass();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            // Check dependencies
            if (dependencies != null) {
                for (CSARDependency dependency : dependencies) {
                    addArchiveToQuery(boolQueryBuilder, element.getDerivedFrom().get(0), dependency.getName(), dependency.getVersion());
                }
            }
            // Check in the archive it-self
            addArchiveToQuery(boolQueryBuilder, element.getDerivedFrom().get(0), archiveName, archiveVersion);
            IndexedInheritableToscaElement superElement = alienDAO.customFind(indexedType, boolQueryBuilder);
            if (superElement == null) {
                throw new IndexingServiceException("Indexing service is in an inconsistent state, the super element [" + element.getDerivedFrom()
                        + "] is not found for element [" + element.getId() + "]");
            }
            IndexedModelUtils.mergeInheritableIndex(superElement, element);
        }
        saveAndUpdateHighestVersion(element);
    }

    private void saveAndUpdateHighestVersion(IndexedToscaElement element) {
        BoolQueryBuilder highestVersionElementQueryBuilder = QueryBuilders.boolQuery();
        QueryBuilder archiveNameMatch = QueryBuilders.termQuery("archiveName", element.getArchiveName());
        QueryBuilder elementIdMatch = QueryBuilders.matchQuery("elementId", element.getElementId().toLowerCase());
        QueryBuilder beHighestVersion = QueryBuilders.termQuery("highestVersion", true);
        highestVersionElementQueryBuilder.must(archiveNameMatch).must(elementIdMatch).must(beHighestVersion);
        IndexedToscaElement highestVersionElement = alienDAO.customFind(element.getClass(), highestVersionElementQueryBuilder);
        if (highestVersionElement != null) {
            int compareVersionResult = VersionUtil.compare(element.getArchiveVersion(), highestVersionElement.getArchiveVersion());
            if (compareVersionResult > 0) {
                // Current version is less recent than mine, I'm the highest version
                highestVersionElement.setHighestVersion(false);
                element.setHighestVersion(true);
                Set<String> currentOlderVersions = highestVersionElement.getOlderVersions();
                Set<String> newOlderVersions = currentOlderVersions != null ? Sets.newHashSet(currentOlderVersions) : new HashSet<String>();
                newOlderVersions.add(highestVersionElement.getArchiveVersion());
                element.setOlderVersions(newOlderVersions);
                highestVersionElement.setOlderVersions(null);
                alienDAO.save(element);
                alienDAO.save(highestVersionElement);
            } else if (compareVersionResult == 0) {
                // The same version as the highest --> override
                element.setHighestVersion(true);
                element.setOlderVersions(highestVersionElement.getOlderVersions());
                alienDAO.save(element);
            } else {
                // Current version is more recent than mine, just save
                Set<String> currentOlderVersions = highestVersionElement.getOlderVersions();
                if (currentOlderVersions == null) {
                    currentOlderVersions = Sets.newHashSet();
                    highestVersionElement.setOlderVersions(currentOlderVersions);
                }
                currentOlderVersions.add(element.getArchiveVersion());
                alienDAO.save(highestVersionElement);
                alienDAO.save(element);
            }
        } else {
            // No element found with other version, I'm the highest version
            element.setHighestVersion(true);
            alienDAO.save(element);
        }
        refreshIndexForSearching();
    }

    private static void addArchiveToQuery(BoolQueryBuilder boolQueryBuilder, String elementId, String archiveName, String archiveVersion) {
        QueryBuilder matchIdQueryBuilder = QueryBuilders.idsQuery().addIds(elementId + ":" + archiveVersion);
        QueryBuilder matchArchiveNameQueryBuilder = QueryBuilders.termQuery("archiveName", archiveName);
        boolQueryBuilder.should(QueryBuilders.boolQuery().must(matchIdQueryBuilder).must(matchArchiveNameQueryBuilder));
    }
}
