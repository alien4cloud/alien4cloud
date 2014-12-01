package alien4cloud.tosca;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.tosca.container.services.csar.ICSARRepositoryIndexerService;
import alien4cloud.tosca.model.ArchiveRoot;

@Component
public class ArchiveIndexer {
    @Resource
    private ICSARRepositoryIndexerService indexerService;

    /**
     * Index an archive in Alien indexed repository.
     * 
     * @param archiveName The name of the archive.
     * @param archiveVersion The version of the archive.
     * @param root The archive root.
     * @param update true if the archive is updated, false if the archive is just indexed.
     */
    public void indexArchive(String archiveName, String archiveVersion, ArchiveRoot root, boolean update) {
        if (update) {
            // get element from the archive so we get the creation date.
            Map<String, IndexedToscaElement> previousElements = indexerService.getArchiveElements(archiveName, archiveVersion);
            prepareForUpdate(archiveName, archiveVersion, root, previousElements);
            // delete all previous elements and their images
            indexerService.deleteElements(previousElements.values());
        }

        performIndexing(archiveName, archiveVersion, root);
    }

    private void prepareForUpdate(String archiveName, String archiveVersion, ArchiveRoot root, Map<String, IndexedToscaElement> previousElements) {
        updateCreationDates(root.getArtifactTypes(), previousElements);
        updateCreationDates(root.getCapabilityTypes(), previousElements);
        updateCreationDates(root.getNodeTypes(), previousElements);
        updateCreationDates(root.getRelationshipTypes(), previousElements);

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                prepareForUpdate(archiveName, archiveVersion, child, previousElements);
            }
        }
    }

    private void updateCreationDates(Map<String, ? extends IndexedInheritableToscaElement> newElements, Map<String, IndexedToscaElement> previousElements) {
        if(newElements == null) {
            return;
        }
        for (IndexedInheritableToscaElement newElement : newElements.values()) {
            IndexedToscaElement previousElement = previousElements.get(newElement.getId());
            if (previousElement != null) {
                newElement.setCreationDate(previousElement.getCreationDate());
            }
        }
    }

    private void performIndexing(String archiveName, String archiveVersion, ArchiveRoot root) {
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getArtifactTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getCapabilityTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getNodeTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getRelationshipTypes(), root.getArchive().getDependencies());

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                performIndexing(archiveName, archiveVersion, child);
            }
        }
    }
}