package org.alien4cloud.bootstrap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import alien4cloud.plugin.PluginManager;
import alien4cloud.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import alien4cloud.FullApplicationConfiguration;
import alien4cloud.audit.rest.AuditController;
import alien4cloud.events.AlienEvent;
import alien4cloud.events.HALeaderElectionEvent;
import alien4cloud.utils.AlienConstants;
import alien4cloud.webconfiguration.RestDocumentationHandlerProvider;
import alien4cloud.webconfiguration.RestDocumentationPluginsBootstrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manage the child context defined in {@link FullApplicationConfiguration}: this context is launched when the current alien instance is elected as leader and
 * eventually destroyed in case of banish.
 */
@Component
@Slf4j
public class ApplicationManager implements ApplicationListener<AlienEvent>, HandlerMapping, PriorityOrdered {
    @Resource
    private ApplicationContext bootstrapContext;

    private AnnotationConfigApplicationContext fullApplicationContext;

    private RequestMappingHandlerMapping mapper;

    private volatile boolean childContextLaunched;

    @Value("#{environment.acceptsProfiles('" + AlienConstants.NO_API_DOC_PROFILE + "')}")
    private boolean apiDocDisabled;

    /**
     * RW Lock for HA Switch
     */
    private ReentrantReadWriteLock switchLock = new ReentrantReadWriteLock();

    /**
     * Lock for event propagation
     */
    private ReentrantLock eventLock = new ReentrantLock();

    /**
     * A synchronized method is enough since the boolean <code>childContextLaunched</code> is only read/write in it.
     */
    @Override
    public void onApplicationEvent(AlienEvent event) {
        if (event instanceof HALeaderElectionEvent) {
            handleHALeaderElectionEvent((HALeaderElectionEvent) event);
        } else {
            try {
                switchLock.readLock().lock();
                handleAlienEvent(event);
            } finally {
                switchLock.readLock().unlock();
            }
        }
    }

    /**
     * receive an event, and forward it to child contexts.
     * Mark it as forwarded before, so that if wont be re-processed here, as an event published into a context is automatically published into the parent
     * context.
     * 
     * @param event
     */
    private void handleAlienEvent(AlienEvent event) {
            if (childContextLaunched) {
                if (!event.isForwarded()) {
                    event.setForwarded(true);
                    // Avoid dispatching the same event twice to a context.

                    try {
                        eventLock.lock();

                        if (!SpringUtils.isSingletonOwnedByContex(fullApplicationContext, event.getSource())) {
                            fullApplicationContext.publishEvent(event);
                        }
                    } finally {
                        eventLock.unlock();
                    }
                } else {
                    log.debug("Event {} already forwarded to children", event);
                }
            } else {
                log.debug("This instance is a backup. It doesn't have to process events.");
            }
    }

    /**
     * configure this host as leader / backup
     * 
     * @param event
     */
    private void handleHALeaderElectionEvent(HALeaderElectionEvent event) {
        // Writes to childContextLaunched need a write lock
        switchLock.writeLock().lock();

        if (event.isLeader()) {
            log.info("Leader Election event received, this instance is now known as the leader");
            if (!childContextLaunched) {
                log.info("Launching the full application context");

                childContextLaunched = true;

                // Downgrade the lock
                switchLock.readLock().lock();
                switchLock.writeLock().unlock();

                try {
                    fullApplicationContext = new AnnotationConfigApplicationContext();
                    fullApplicationContext.setParent(this.bootstrapContext);
                    fullApplicationContext.setId(this.bootstrapContext.getId() + ":full");
                    fullApplicationContext.register(FullApplicationConfiguration.class);
                    fullApplicationContext.refresh();
                    fullApplicationContext.start();

                    mapper = new RequestMappingHandlerMapping();
                    mapper.setApplicationContext(fullApplicationContext);
                    mapper.afterPropertiesSet();

                    refreshDocumentation();

                    AuditController auditController = fullApplicationContext.getBean(AuditController.class);
                    auditController.register(mapper);
                } finally {
                    switchLock.readLock().unlock();
                }

            } else {
                // Nothing to do, release the lock
                switchLock.writeLock().unlock();
                log.warn("The full application context is already launched, something seems wrong in the current state !");
            }
        } else {
            if (childContextLaunched) {
                childContextLaunched = false;

                // Downgrade the lock
                switchLock.readLock().lock();
                switchLock.writeLock().unlock();

                try {
                    Object pluginManagerBean = fullApplicationContext.getBean("plugin-manager");
                    if (pluginManagerBean != null) {
                        PluginManager pluginManager = (PluginManager) pluginManagerBean;
                        log.info("Unloading all plugin before Destroying the full application context");
                        try {
                            pluginManager.unloadAllPlugins();
                        } catch (Exception e) {
                            log.error("Not able to unload plugins", e);
                        }
                    } else {
                        log.warn("plugin-manager can not be found");
                    }
                    log.info("Destroying the full application context");

                    mapper = null;
                    fullApplicationContext.destroy();
                } finally {
                    switchLock.readLock().unlock();
                }
            } else {
                // Nothing to do, release the lock
                switchLock.writeLock().unlock();

                log.warn("The full application context is already destroyed, something seems wrong in the current state !");
            }
        }
    }

    private void refreshDocumentation() {
        if (!apiDocDisabled) {
            RestDocumentationHandlerProvider restDocumentationHandlerProvider = fullApplicationContext.getBean(RestDocumentationHandlerProvider.class);
            restDocumentationHandlerProvider.register(mapper);
            RestDocumentationPluginsBootstrapper documentationPluginsBootstrapper = fullApplicationContext.getBean(RestDocumentationPluginsBootstrapper.class);
            documentationPluginsBootstrapper.refresh();
        }
    }

    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        if (mapper != null) {
            return mapper.getHandler(request);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }

}
