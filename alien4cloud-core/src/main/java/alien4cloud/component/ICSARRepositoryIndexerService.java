package alien4cloud.component;

import java.util.Collection;
import java.util.Map;

import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedToscaElement;

/**
 * Service responsible for indexing TOSCA elements.
 */
public interface ICSARRepositoryIndexerService {
    /**
     * Get all {@link IndexedToscaElement} from a given archive.
     * 
     * @param archiveName The name of the archive.
     * @param archiveVersion The version of the archive.
     * @return A map of {@link IndexedToscaElement} by id.
     */
    Map<String, IndexedToscaElement> getArchiveElements(String archiveName, String archiveVersion);

    /**
     * Delete all elements from a given archive.
     * 
     * @param archiveName The name of the archive to delete.
     * @param archiveVersion The version of the archive to delete.
     */
    void deleteElements(String archiveName, String archiveVersion);

    /**
     * Index multiple elements into the repository.
     * 
     * @param archiveName The name of the archive in which the elements lies.
     * @param archiveVersion The version of the archive in which the elements lies.
     * @param archiveElements The elements to index.
     * @param dependencies The archive dependencies (in order to add infos from the parent element to the child...)
     */
    void indexInheritableElements(String archiveName, String archiveVersion, Map<String, ? extends IndexedInheritableToscaElement> archiveElements,
            Collection<CSARDependency> dependencies);

    /**
     * Index a single element into the repository.
     * 
     * @param archiveName The name of the archive in which the elements lies.
     * @param archiveVersion The version of the archive in which the elements lies.
     * @param element The element to index.
     * @param dependencies The archive dependencies (in order to add infos from the parent element to the child...)
     */
    void indexInheritableElement(String archiveName, String archiveVersion, IndexedInheritableToscaElement element, Collection<CSARDependency> dependencies);

    /**
     * Delete the given elements from the repository.
     * 
     * @param elements the elements to delete.
     */
    void deleteElements(Collection<IndexedToscaElement> elements);

    <T extends IndexedToscaElement> Map<String, T> getArchiveElements(String archiveName, String archiveVersion, Class<T> type);
}