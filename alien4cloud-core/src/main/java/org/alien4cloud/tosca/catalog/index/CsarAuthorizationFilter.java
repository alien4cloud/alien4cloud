package org.alien4cloud.tosca.catalog.index;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.Csar;
import org.springframework.stereotype.Component;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;

@Component
public class CsarAuthorizationFilter implements ICsarAuthorizationFilter {
    @Inject
    private ITopologyCatalogService topologyCatalogService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;

    @Override
    public void checkWriteAccess(Csar csar) {
        // if this csar has node types, check the COMPONENTS_MANAGER Role
        if (toscaTypeSearchService.hasTypes(csar.getName(), csar.getVersion())) {
            AuthorizationUtil.checkHasOneRoleIn(Role.COMPONENTS_MANAGER);
        }
        // if the csar is bound to a topology, check the ARCHITECT Role
        if (topologyCatalogService.exists(csar.getId())) {
            AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        }
    }

    @Override
    public void checkReadAccess(Csar csar) {
        // Component browser has access to Csar
        AuthorizationUtil.checkHasOneRoleIn(Role.COMPONENTS_BROWSER);
    }
}
