package org.alien4cloud.tosca.catalog.index;

import java.util.Collection;
import java.util.Map;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;

/**
 * Service responsible for indexing TOSCA elements.
 */
public interface IToscaTypeIndexerService {
    /**
     * Get all {@link AbstractToscaType} from a given archive.
     * 
     * @param archiveName The name of the archive.
     * @param archiveVersion The version of the archive.
     * @return A map of {@link AbstractToscaType} by id.
     */
    Map<String, AbstractToscaType> getArchiveElements(String archiveName, String archiveVersion);

    /**
     * Get all elements from a given archive with the given type
     * 
     * @param archiveName The name of the archive.
     * @param archiveVersion The version of the archive.
     * @param type type of the requested elements
     * @param <T> type of element
     * @return all element with the given type within the archive
     */
    <T extends AbstractToscaType> Map<String, T> getArchiveElements(String archiveName, String archiveVersion, Class<T> type);

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
     * @param archiveElements The elements to index.
     * @param dependencies The archive dependencies (in order to add infos from the parent element to the child...)
     */
    void indexInheritableElements(Map<String, ? extends AbstractInheritableToscaType> archiveElements,
            Collection<CSARDependency> dependencies);

    /**
     * Index a single element into the repository.
     * 
     * @param archiveName The name of the archive in which the elements lies.
     * @param archiveVersion The version of the archive in which the elements lies.
     * @param element The element to index.
     * @param dependencies The archive dependencies (in order to add infos from the parent element to the child...)
     */
    void indexInheritableElement(String archiveName, String archiveVersion, AbstractInheritableToscaType element, Collection<CSARDependency> dependencies);

    /**
     * Delete the given elements from the repository.
     * 
     * @param elements the elements to delete.
     */
    void deleteElements(Collection<AbstractToscaType> elements);
}