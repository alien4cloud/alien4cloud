package org.alien4cloud.tosca.catalog.index;

import org.alien4cloud.tosca.model.Csar;

import alien4cloud.plugin.aop.Overridable;

public interface ICsarAuthorizationFilter {

    @Overridable
    void checkWriteAccess(Csar csar);

    @Overridable
    void checkReadAccess(Csar csar);
}
