package org.alien4cloud.tosca.editor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ICsarAuthorizationFilter;
import org.springframework.stereotype.Component;

import alien4cloud.webconfiguration.StaticResourcesConfiguration;
import lombok.extern.slf4j.Slf4j;

/**
 * Defer the injection of the editor service into static resource configuration bean.
 */
@Slf4j
@Component
public class EditorStaticResourcesConfigurer {
    @Inject
    private ICsarAuthorizationFilter csarAuthorizationFilter;
    @Inject
    private StaticResourcesConfiguration configuration;

    @PostConstruct
    public void register() {
        log.info("Initializing context: linking editor service from static resource provider.");
        configuration.setCsarAuthorizationFilter(csarAuthorizationFilter);
    }

    @PreDestroy
    public void unregister() {
        log.info("Destroying context: unlinking editor service from static resource provider.");
        configuration.setCsarAuthorizationFilter(null);
    }
}