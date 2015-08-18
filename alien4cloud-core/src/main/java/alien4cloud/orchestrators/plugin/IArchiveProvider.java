package alien4cloud.orchestrators.plugin;

import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Instances of IArchiveProvider must initialize an Cloud Service Archives that represents TOSCA nodes that the provider can manage.
 */
public interface IArchiveProvider {
    /**
     * Provides an archive to be indexed by Alien 4 Cloud.
     *
     * @return The archive to be indexed by Alien 4 Cloud and published in global repository.
     */
    ArchiveRoot getArchive();
}