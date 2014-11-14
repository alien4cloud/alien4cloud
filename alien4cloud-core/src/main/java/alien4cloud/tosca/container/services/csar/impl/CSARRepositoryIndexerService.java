package alien4cloud.tosca.container.services.csar.impl;

import java.util.Collection;
import java.util.Date;
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
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.ToscaInheritableElement;
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
        elasticSearchClient.getClient().admin().indices().prepareRefresh(ToscaElement.class.getSimpleName().toLowerCase()).execute().actionGet();
    }

    @Override
    public void indexElements(String archiveName, String archiveVersion, Map<String, ToscaElement> archiveElements) {
        for (ToscaElement element : archiveElements.values()) {
            IndexedToscaElement indexedElement = IndexedModelUtils.getNonInheritableIndexedModel(element, archiveName, archiveVersion);
            saveAndUpdateHighestVersion(indexedElement);
        }
    }

    @Override
    public void indexInheritableElements(String archiveName, String archiveVersion, Map<String, ToscaInheritableElement> archiveElements,
            Collection<CSARDependency> dependencies) {
        List<ToscaInheritableElement> orderedElements = IndexedModelUtils.orderForIndex(archiveElements);
        for (ToscaInheritableElement element : orderedElements) {
            indexInheritableElement(archiveName, archiveVersion, element, dependencies);
        }
    }

    public void indexInheritableElement(String archiveName, String archiveVersion, ToscaInheritableElement element, Collection<CSARDependency> dependencies) {

        IndexedNodeType indexedNodeType = getExistingIndexedNodeType(archiveVersion, element);
        Date creationDate = (indexedNodeType != null) ? indexedNodeType.getCreationDate() : null;

        IndexedInheritableToscaElement indexedElement = IndexedModelUtils.getInheritableIndexedModel(element, archiveName, archiveVersion, creationDate);
        if (element.getDerivedFrom() != null) {
            Class<? extends IndexedInheritableToscaElement> indexedType = IndexedModelUtils.getInheritableIndexClass(element.getClass());
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            // Check dependencies
            for (CSARDependency dependency : dependencies) {
                addArchiveToQuery(boolQueryBuilder, element.getDerivedFrom(), dependency.getName(), dependency.getVersion());
            }
            // Check in the archive it-self
            addArchiveToQuery(boolQueryBuilder, element.getDerivedFrom(), archiveName, archiveVersion);
            IndexedInheritableToscaElement superElement = alienDAO.customFind(indexedType, boolQueryBuilder);
            if (superElement == null) {
                throw new IndexingServiceException("Indexing service is in an inconsistent state, the super element [" + element.getDerivedFrom()
                        + "] is not found for element [" + element.getId() + "]");
            }
            IndexedModelUtils.mergeInheritableIndex(superElement, indexedElement);
        }
        saveAndUpdateHighestVersion(indexedElement);
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

    // TODO : This method should be removed once we manage correctly csar dependencies (for snapshot CSAR edited in ALIEN).
    @Override
    public void indexInheritableElement(String archiveName, String archiveVersion, ToscaInheritableElement element) {

        IndexedNodeType indexedNodeType = getExistingIndexedNodeType(archiveVersion, element);
        Date creationDate = (indexedNodeType != null) ? indexedNodeType.getCreationDate() : null;

        IndexedInheritableToscaElement indexedElement = IndexedModelUtils.getInheritableIndexedModel(element, archiveName, archiveVersion, creationDate);
        if (element.getDerivedFrom() != null) {
            Class<? extends IndexedInheritableToscaElement> indexedType = IndexedModelUtils.getInheritableIndexClass(element.getClass());
            QueryBuilder matchElementIdQueryBuilder = QueryBuilders.matchQuery("elementId", element.getDerivedFrom().toLowerCase());
            IndexedInheritableToscaElement superElement = alienDAO.customFind(indexedType, matchElementIdQueryBuilder);
            if (superElement == null) {
                throw new IndexingServiceException("Super element not found [" + element.getDerivedFrom() + "]");
            }
            IndexedModelUtils.mergeInheritableIndex(superElement, indexedElement);
        }
        saveAndUpdateHighestVersion(indexedElement);
    }

    /**
     * Recover an existing indexedNodeType
     * 
     * @param archiveVersion
     * @param element
     * @return
     */
    private IndexedNodeType getExistingIndexedNodeType(String archiveVersion, ToscaInheritableElement element) {
        String indexedNodeTypeId = element.getId() + ":" + archiveVersion;
        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, indexedNodeTypeId);
        return indexedNodeType;
    }

    public void deleteElement(String archiveName, String archiveVersion, ToscaElement element) {
        alienDAO.delete(IndexedModelUtils.getIndexClass(element.getClass()), element.getId() + ":" + archiveVersion);
    }

    private static void addArchiveToQuery(BoolQueryBuilder boolQueryBuilder, String elementId, String archiveName, String archiveVersion) {
        QueryBuilder matchIdQueryBuilder = QueryBuilders.idsQuery().addIds(elementId + ":" + archiveVersion);
        QueryBuilder matchArchiveNameQueryBuilder = QueryBuilders.termQuery("archiveName", archiveName);
        boolQueryBuilder.should(QueryBuilders.boolQuery().must(matchIdQueryBuilder).must(matchArchiveNameQueryBuilder));
    }
}
