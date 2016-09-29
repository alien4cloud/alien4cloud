package org.alien4cloud.tosca.catalog.index;

import org.springframework.stereotype.Component;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.model.ArchiveRoot;

/**
 * This bean is responsible for managing authorization when an archive is indexed.
 */
@Component
public class ArchiveIndexerAuthorizationFilter implements IArchiveIndexerAuthorizationFilter {
    @Override
    public void checkAuthorization(ArchiveRoot archiveRoot) {
        if (archiveRoot.hasToscaTopologyTemplate()) {
            AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT, Role.ADMIN);
        }
        if (archiveRoot.hasToscaTypes()) {
            AuthorizationUtil.checkHasOneRoleIn(Role.COMPONENTS_MANAGER, Role.ADMIN);
        }
    }
}