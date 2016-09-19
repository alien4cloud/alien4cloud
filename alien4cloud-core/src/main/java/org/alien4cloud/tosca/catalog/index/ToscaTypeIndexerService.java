package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.images.IImageDAO;
import alien4cloud.model.common.Tag;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.utils.MapUtil;

/**
 * This service is responsible for indexing and searching tosca types.
 */
@Service
public class ToscaTypeIndexerService implements IToscaTypeIndexerService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ElasticSearchClient elasticSearchClient;
    @Inject
    private IImageDAO imageDAO;
    @Inject
    private ToscaTypeSearchService searchService;

    private void refreshIndexForSearching() {
        elasticSearchClient.getClient().admin().indices().prepareRefresh(ElasticSearchDAO.TOSCA_ELEMENT_INDEX).execute().actionGet();
    }

    @Override
    public Map<String, AbstractToscaType> getArchiveElements(String archiveName, String archiveVersion, String workspace) {
        return getArchiveElements(archiveName, archiveVersion, workspace, AbstractToscaType.class);
    }

    @Override
    public <T extends AbstractToscaType> Map<String, T> getArchiveElements(String archiveName, String archiveVersion, String workspaceId, Class<T> type) {
        GetMultipleDataResult<T> elements = alienDAO.find(type, MapUtil.newHashMap(new String[] { "archiveName", "archiveVersion" },
                new String[][] { new String[] { archiveName }, new String[] { archiveVersion } }), Integer.MAX_VALUE);

        Map<String, T> elementsByIds = Maps.newHashMap();
        if (elements == null) {
            return elementsByIds;
        }

        for (T element : elements.getData()) {
            elementsByIds.put(element.getId(), element);
        }
        return elementsByIds;
    }

    @Override
    public void deleteElements(String name, String version, String workspace) {
        GetMultipleDataResult<AbstractToscaType> result = alienDAO.buildQuery(AbstractToscaType.class)
                .setFilters(fromKeyValueCouples("archiveName", name, "archiveVersion", version, "workspace", workspace)).prepareSearch()
                .setFetchContext(FetchContext.SUMMARY).search(0, Integer.MAX_VALUE);

        AbstractToscaType[] elements = result.getData();

        // we need to delete each element
        for (AbstractToscaType element : elements) {
            deleteElement(element);
        }
    }

    @Override
    public void indexInheritableElements(String archiveName, String archiveVersion, String workspace,
            Map<String, ? extends AbstractInheritableToscaType> archiveElements, Collection<CSARDependency> dependencies) {
        for (AbstractInheritableToscaType element : safe(archiveElements).values()) {
            element.setLastUpdateDate(new Date());
            Date creationDate = element.getCreationDate() == null ? element.getLastUpdateDate() : element.getCreationDate();
            element.setCreationDate(creationDate);
            element.setWorkspace(workspace);
            alienDAO.save(element);
        }
        refreshIndexForSearching();
    }

    @Override
    public void indexInheritableElement(String archiveName, String archiveVersion, AbstractInheritableToscaType element,
            Collection<CSARDependency> dependencies) {
        // FIXME do we need all the merge in case of substitution ?
        element.setLastUpdateDate(new Date());
        Date creationDate = element.getCreationDate() == null ? element.getLastUpdateDate() : element.getCreationDate();
        element.setCreationDate(creationDate);
        if (element.getDerivedFrom() != null && element.getDerivedFrom().size() > 0) {
            boolean deriveFromSimpleType = false;
            String parentId = element.getDerivedFrom().get(0);
            if (element.getDerivedFrom().size() == 1 && ToscaType.isSimple(parentId)) {
                deriveFromSimpleType = true;
            }
            if (!deriveFromSimpleType) {
                Set<CSARDependency> allDependencies = Sets.newHashSet(dependencies);
                allDependencies.add(new CSARDependency(archiveName, archiveVersion));
                AbstractInheritableToscaType superElement = searchService.getElementInDependencies(element.getClass(), parentId, allDependencies);
                if (superElement == null) {
                    throw new IndexingServiceException("Indexing service is in an inconsistent state, the super element [" + element.getDerivedFrom()
                            + "] is not found for element [" + element.getId() + "]");
                }
                IndexedModelUtils.mergeInheritableIndex(superElement, element);
            }
        }
        alienDAO.save(element);
    }

    private void deleteElement(AbstractToscaType element) {
        Tag iconTag = ArchiveImageLoader.getIconTag(element.getTags());
        if (iconTag != null) {
            imageDAO.delete(iconTag.getValue());
        }
        alienDAO.delete(element.getClass(), element.getId());
    }

    @Override
    public void deleteElements(Collection<AbstractToscaType> elements) {
        for (AbstractToscaType element : elements) {
            alienDAO.delete(element.getClass(), element.getId());
        }
    }
}