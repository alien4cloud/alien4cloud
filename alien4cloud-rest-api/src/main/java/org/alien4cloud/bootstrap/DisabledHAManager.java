package org.alien4cloud.bootstrap;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import alien4cloud.events.HALeaderElectionEvent;

/**
 * When HA is not enabled, we need to wake up everything on this instance as if it was the leader.
 */
@Component
@Slf4j
// @Bootstrapable
public class DisabledHAManager implements ApplicationListener<EmbeddedServletContainerInitializedEvent> {

    @Value("${ha.ha_enabled:#{false}}")
    private boolean haEnabled;

    @Resource
    private ApplicationContext alienContext;

    @Override
    public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
        if (!haEnabled) {
            log.info("HA is not enabled, this instance is the leader de facto");
            alienContext.publishEvent(new HALeaderElectionEvent(this, true));
        }
    }

}
