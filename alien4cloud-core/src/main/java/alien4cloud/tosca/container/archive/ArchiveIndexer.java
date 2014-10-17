package alien4cloud.tosca.container.archive;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.services.csar.ICSARRepositoryIndexerService;

@Component
public class ArchiveIndexer {

    @Resource
    private ICSARRepositoryIndexerService indexerService;

    /**
     * Index a cloud service archive
     * 
     * @param archive to be indexed
     */
    public void indexArchive(CloudServiceArchive archive) {
        indexerService.indexInheritableElements(archive.getMeta().getName(), archive.getMeta().getVersion(), archive.getArchiveInheritableElements(), archive
                .getMeta().getDependencies());
        indexerService.indexElements(archive.getMeta().getName(), archive.getMeta().getVersion(), archive.getArchiveNonInheritableElements());
    }
}
