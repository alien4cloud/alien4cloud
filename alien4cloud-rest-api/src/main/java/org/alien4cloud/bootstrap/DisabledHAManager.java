package org.alien4cloud.bootstrap;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import alien4cloud.events.HALeaderElectionEvent;

/**
 * When HA is not enabled, we need to wake up everything on this instance as if it was the leader.
 *
 * TODO It may be better/simpler to process EmbeddedServletContainerInitializedEvent rather than Context
 */
@Component
@Slf4j
public class DisabledHAManager implements ApplicationListener<ContextRefreshedEvent> {

    @Value("${ha.ha_enabled:#{false}}")
    private boolean haEnabled;

    @Resource
    private ApplicationContext alienContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!haEnabled && event.getApplicationContext() == alienContext) {
            // child context may also dispatch context refresh event, we just want to process the ones of the main context.
            log.info("HA is not enabled, this instance is the leader de facto");
            alienContext.publishEvent(new HALeaderElectionEvent(this, true));
        }
    }
}