package org.alien4cloud.tosca.catalog.index;

import org.alien4cloud.tosca.model.Csar;

import alien4cloud.plugin.aop.Overridable;

public interface ICsarAuthorizationFilter {

    /**
     * Check write access for the current user on the given CSAR
     * 
     * @param csar the csar to check for authorization
     */
    @Overridable
    void checkWriteAccess(Csar csar);

    /**
     * Check read access for the current user on the given CSAR
     * 
     * @param csar the csar to check for authorization
     */
    @Overridable
    void checkReadAccess(Csar csar);
}
