package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.images.IImageDAO;
import alien4cloud.model.common.Tag;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.normative.ToscaType;

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
    private IToscaTypeSearchService searchService;

    private void refreshIndexForSearching() {
        elasticSearchClient.getClient().admin().indices().prepareRefresh(ElasticSearchDAO.TOSCA_ELEMENT_INDEX).execute().actionGet();
    }

    @Override
    public Map<String, AbstractToscaType> getArchiveElements(String archiveName, String archiveVersion) {
        return getArchiveElements(archiveName, archiveVersion, AbstractToscaType.class);
    }

    @Override
    public <T extends AbstractToscaType> Map<String, T> getArchiveElements(String archiveName, String archiveVersion, Class<T> type) {
        GetMultipleDataResult<T> elements = alienDAO.buildQuery(type)
                .setFilters(fromKeyValueCouples("archiveName", archiveName, "archiveVersion", archiveVersion)).prepareSearch().search(0, Integer.MAX_VALUE);

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
    public void deleteElements(String name, String version) {
        GetMultipleDataResult<AbstractToscaType> result = alienDAO.buildQuery(AbstractToscaType.class)
                .setFilters(fromKeyValueCouples("archiveName", name, "archiveVersion", version)).prepareSearch().setFetchContext(FetchContext.SUMMARY)
                .search(0, Integer.MAX_VALUE);

        AbstractToscaType[] elements = result.getData();

        // we need to delete each element
        for (AbstractToscaType element : elements) {
            deleteElement(element);
        }
    }

    @Override
    public void indexInheritableElements(Map<String, ? extends AbstractInheritableToscaType> archiveElements, Collection<CSARDependency> dependencies) {
        for (AbstractInheritableToscaType element : safe(archiveElements).values()) {
            alienDAO.save(element);
        }
        refreshIndexForSearching();
    }

    @Override
    @ToscaContextual
    public void indexInheritableElement(String archiveName, String archiveVersion, AbstractInheritableToscaType element,
            Collection<CSARDependency> dependencies) {
        // FIXME do we need all the merge in case of substitution ?
        if (CollectionUtils.isNotEmpty(element.getDerivedFrom())) {
            boolean deriveFromSimpleType = false;
            String parentId = element.getDerivedFrom().get(0);
            if (element.getDerivedFrom().size() == 1 && ToscaType.isSimple(parentId)) {
                deriveFromSimpleType = true;
            }
            if (!deriveFromSimpleType) {
                AbstractInheritableToscaType superElement = ToscaContext.getOrFail(element.getClass(), parentId);
                IndexedModelUtils.mergeInheritableIndex(superElement, element);
            }
        }

        alienDAO.save(element);
        refreshIndexForSearching();
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