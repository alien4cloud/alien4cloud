package alien4cloud.tosca;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.container.services.csar.ICSARRepositoryIndexerService;
import alien4cloud.tosca.model.ArchiveRoot;

@Component
public class ArchiveIndexer {
    @Resource
    private ICSARRepositoryIndexerService indexerService;

    public void indexArchive(ArchiveRoot root) {
        indexerService.indexInheritableElements(root.getArchive().getName(), root.getArchive().getVersion(), root.getArtifactTypes(), root.getArchive()
                .getDependencies());
        indexerService.indexInheritableElements(root.getArchive().getName(), root.getArchive().getVersion(), root.getCapabilityTypes(), root.getArchive()
                .getDependencies());
        indexerService.indexInheritableElements(root.getArchive().getName(), root.getArchive().getVersion(), root.getNodeTypes(), root.getArchive()
                .getDependencies());
        indexerService.indexInheritableElements(root.getArchive().getName(), root.getArchive().getVersion(), root.getRelationshipTypes(), root.getArchive()
                .getDependencies());

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                indexArchive(child);
            }
        }
    }
}