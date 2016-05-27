package alien4cloud.tosca.repository;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedToscaElement;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Manages a file-based local archive repository.
 *
 * FIXME implementation.
 */
@Component
public class LocalRepositoryImpl implements ICSARRepositorySearchService {
    /** Path of the local repository. */
    private Path localRepositoryPath;

    @Override
    public Csar getArchive(String id) {
        return null;
    }

    @Override
    public boolean isElementExistInDependencies(Class<? extends IndexedToscaElement> elementClass, String elementId, Collection<CSARDependency> dependencies) {
        return false;
    }

    @Override
    public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies) {
        return null;
    }

    @Override
    public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, QueryBuilder query, Collection<CSARDependency> dependencies) {
        return null;
    }

    @Override
    public <T extends IndexedToscaElement> T getRequiredElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies)
            throws NotFoundException {
        return null;
    }

    @Override
    public <T extends IndexedToscaElement> T getParentOfElement(Class<T> elementClass, T indexedToscaElement, String parentElementId) {
        return null;
    }
}