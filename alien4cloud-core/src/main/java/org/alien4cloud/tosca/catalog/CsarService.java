package org.alien4cloud.tosca.catalog;

import org.alien4cloud.tosca.model.Csar;

/**
 * Service to manage cloud service archives.
 */
public class CsarService {
    /**
     * Create a new archive in the catalog. The archive must not exists in any visible workspaces.
     *
     * @param archiveName
     * @param archiveVersion
     */
    public void createNewCsar(String archiveName, String archiveVersion, String delegateType, String delegateId) {
        Csar csar = new Csar();
    }

    /**
     *
     * @param archiveName
     * @param archiveVersion
     * @param archiveHash
     */
    public void saveArchive(String archiveName, String archiveVersion, String archiveHash) {

    }
}
