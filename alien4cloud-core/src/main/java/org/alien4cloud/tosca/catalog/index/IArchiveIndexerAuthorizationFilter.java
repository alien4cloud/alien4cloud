package org.alien4cloud.tosca.catalog.index;

import alien4cloud.plugin.aop.Overridable;
import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Check authorization when an archive is indexed in the repository.
 */
public interface IArchiveIndexerAuthorizationFilter {

    @Overridable
    void checkAuthorization(ArchiveRoot archiveRoot);
}