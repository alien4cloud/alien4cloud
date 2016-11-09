package org.alien4cloud.tosca.catalog.index;

import alien4cloud.plugin.aop.Overridable;
import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Check authorization when an archive is indexed in the repository.
 */
public interface IArchiveIndexerAuthorizationFilter {

    /**
     * Pre-check authorization to upload to the given workspace. This is because before parsing we don't know yet if the archive contains topology or types.
     * The more detailed check based on the content of the CSAR should be done later with checkAuthorization method
     * 
     * @param workspace the workspace to check for authorization
     */
    @Overridable
    void preCheckAuthorization(String workspace);

    /**
     * Check authorization to upload to the given workspace with the given archive.
     * If the archive contains topology then ARCHITECT role is necessary.
     * If the archive contains types then COMPONENT_MANAGER role is necessary.
     * 
     * @param archiveRoot the archive to check for authorization
     */
    @Overridable
    void checkAuthorization(ArchiveRoot archiveRoot);
}