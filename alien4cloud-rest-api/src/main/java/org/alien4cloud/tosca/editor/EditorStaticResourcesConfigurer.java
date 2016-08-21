package org.alien4cloud.tosca.editor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import alien4cloud.webconfiguration.StaticResourcesConfiguration;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Defer the injection of the editor service into static resource configuration bean.
 */
@Slf4j
@Component
public class EditorStaticResourcesConfigurer {
    @Inject
    private EditorService editorService;
    @Inject
    private StaticResourcesConfiguration configuration;

    @PostConstruct
    public void register() {
        log.info("Initializing context: linking editor service from static resource provider.");
        configuration.setEditorService(editorService);
    }

    @PreDestroy
    public void unregister() {
        log.info("Destroying context: unlinking editor service from static resource provider.");
        configuration.setEditorService(null);
    }
}