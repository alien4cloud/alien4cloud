package alien4cloud.rest.health;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import alien4cloud.events.HALeaderElectionEvent;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.Consul.Builder;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.async.ConsulResponseCallback;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.Session;
import com.orbitz.consul.model.session.SessionInfo;
import com.orbitz.consul.option.QueryOptions;

@Component
@Slf4j
@Deprecated
// will be moved in alien4cloud-ha plugin
public class HAManager implements ApplicationListener<EmbeddedServletContainerInitializedEvent> {

    private static final String SERVICE_NAME = "a4c";

    private static final String LEARDER_KEY = "service/" + SERVICE_NAME + "/leader";

    @Value("${ha.ha_enabled:#{false}}")
    private boolean haEnabled;

    @Value("${ha.consulAgentIp:#{null}}")
    private String consulAgentIp;

    @Value("${ha.consulAgentPort:#{0}}")
    private int consulAgentPort;

    private String instanceId;

    @Value("${ha.instanceIp:#{null}}")
    private String instanceIp;

    @Value("${ha.healthCheckPeriodInSecond:#{5}}")
    private long healthCheckPeriodInSecond;

    @Value("${ha.consulSessionTTLInSecond:#{1800}}")
    private long consulSessionTTLInSecond;

    @Value("${ha.consulLockDelayInSecond:#{15}}")
    private long consulLockDelayInSecond;

    @Value("${ha.consulQueryTimeoutInMin:#{3}}")
    private int consulQueryTimeoutInMin;

    @Value("${ha.consulConnectTimeoutInMillis:#{1000 * 30}}" /* default 30 s */)
    private long consulConnectTimeoutInMillis;

    @Value("${ha.consulReadTimeoutInMillis:#{1000 * 60 * 5}}" /* default 5 min */)
    private long consulReadTimeoutInMillis;

    @Resource
    private ApplicationContext alienContext;

    private int listenPort;

    private Consul consul;

    private String checkId;

    private volatile String sessionId;
    private ReentrantLock sessionLock = new ReentrantLock();

    private volatile boolean leader;
    private ReentrantLock leaderLock = new ReentrantLock();

    private ThreadPoolTaskScheduler sessionRenewerTaskScheduler;

    private ThreadPoolTaskScheduler consulLockAquisitionTaskScheduler;

    @PostConstruct
    public void init() {
        if (!haEnabled) {
            return;
        }

        Builder consulBuilder = Consul.builder();
        if (consulAgentIp != null && consulAgentPort > 0) {
            consulBuilder = consulBuilder.withHostAndPort(HostAndPort.fromParts(consulAgentIp, consulAgentPort));
            if (log.isDebugEnabled()) {
                log.debug("Will connect to consul using ip <{}> and port <{}>");
            }
        } else {
            // if consulAgentIp && consulAgentPort are not specified, connect to localhost agent
            if (log.isDebugEnabled()) {
                log.debug("Will connect to localhost consul");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Will connect to consul using {}ms connect timeout and {}ms read timeout", consulConnectTimeoutInMillis,
                    consulReadTimeoutInMillis);
        }
        consulBuilder = consulBuilder.withReadTimeoutMillis(consulReadTimeoutInMillis);
        consulBuilder = consulBuilder.withConnectTimeoutMillis(consulConnectTimeoutInMillis);
        consul = consulBuilder.build();

        this.instanceId = instanceIp; // UUID.randomUUID().toString();
        this.checkId = "a4cHealthCheck:" + instanceId;

        sessionRenewerTaskScheduler = new ThreadPoolTaskScheduler();
        sessionRenewerTaskScheduler.setPoolSize(1);
        sessionRenewerTaskScheduler.setThreadNamePrefix("consul-session-renewer-");
        sessionRenewerTaskScheduler.setDaemon(true);
        sessionRenewerTaskScheduler.initialize();

        consulLockAquisitionTaskScheduler = new ThreadPoolTaskScheduler();
        consulLockAquisitionTaskScheduler.setPoolSize(1);
        consulLockAquisitionTaskScheduler.setThreadNamePrefix("consul-lock-aquisition-");
        consulLockAquisitionTaskScheduler.setDaemon(true);
        consulLockAquisitionTaskScheduler.initialize();
    }

    private void elect() {
        leaderLock.lock();
        try {
            if (leader) {
                if (log.isTraceEnabled()) {
                    log.trace("Already leader, nothing to do !");
                }
                return;
            } else {
                log.info("Becoming leader for the resource {}", LEARDER_KEY);
                leader = true;
                alienContext.publishEvent(new HALeaderElectionEvent(this, leader));
            }
        } finally {
            leaderLock.unlock();
        }
    }

    private void banish() {
        leaderLock.lock();
        try {
            if (!leader) {
                if (log.isTraceEnabled()) {
                    log.trace("Already banished, nothing to do !");
                }
                return;
            } else {
                log.info("I'm not more the leader for the resource {}", LEARDER_KEY);
                leader = false;
                alienContext.publishEvent(new HALeaderElectionEvent(this, leader));
            }
        } finally {
            leaderLock.unlock();
        }
    }

    @PreDestroy
    public void dispose() {
        if (!haEnabled) {
            return;
        }
        if (sessionId != null) {
            try {
                consul.sessionClient().destroySession(sessionId);
            } catch (Exception e) {
                log.warn("Not able to destroy session on shutdown", e);
            }
        }
        try {
            consul.agentClient().deregisterCheck(this.checkId);
        } catch (Exception e) {
            log.warn("Not able to unregister check on shutdown", e);
        }
        try {
            consulLockAquisitionTaskScheduler.destroy();
        } catch (Exception e) {
            log.warn("Not able to destroy lock acquisition task schedudler", e);
        }
        try {
            sessionRenewerTaskScheduler.destroy();
        } catch (Exception e) {
            log.warn("Not able to destroy session renewer task schedudler", e);
        }
    }

    private void initRegistration() throws MalformedURLException {
        if (log.isDebugEnabled()) {
            log.debug("Registering consul check this id <{}>, name <{}> on url <{}> every {} seconds", this.checkId, SERVICE_NAME, getCheckUrl(),
                    healthCheckPeriodInSecond);
        }

        com.orbitz.consul.model.agent.ImmutableRegistration.Builder builder = ImmutableRegistration.builder();
        builder.address(this.instanceIp);
        builder.port(listenPort);
        builder.name(SERVICE_NAME);
        builder.id(this.checkId);
        builder.check(Registration.RegCheck.http(getCheckUrl().toExternalForm(), healthCheckPeriodInSecond));
        ImmutableRegistration registration = builder.build();
        consul.agentClient().register(registration);

        // consul.agentClient().registerCheck(this.checkId, SERVICE_NAME, getCheckUrl(), healthCheckPeriodInSecond);
        
        // we start the session renewer (that will also create the session after 2 health checks)
        Date sessionRenewStartDate = new Date(System.currentTimeMillis() + (2* healthCheckPeriodInSecond * 1000));
        // we renew the session at the half time of it's TTL
        long renewSessionSchedulePeriod = consulSessionTTLInSecond * 1000 / 2;
        sessionRenewerTaskScheduler.scheduleAtFixedRate(new SessionRenewer(), sessionRenewStartDate, renewSessionSchedulePeriod);

        consulLockAquisitionTaskScheduler.schedule(new LockAquisition(), sessionRenewStartDate);
        // consulLockAquisitionTaskScheduler.scheduleAtFixedRate(new LockAquisition(), consulLeaderElectionPeriodInSecond * 1000);
    }

    private URL getCheckUrl() throws MalformedURLException {
        return new URL("http://" + instanceIp + ":" + listenPort + "/rest/latest/health/check");
    }

    @Override
    public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
        log.info("EmbeddedServletContainer is ready, listenning on port {}", event.getEmbeddedServletContainer().getPort());
        this.listenPort = event.getEmbeddedServletContainer().getPort();
        if (!haEnabled) {
            log.info("HA is not enabled");
            return;
        }
        try {
            initRegistration();
        } catch (MalformedURLException e) {
            log.error("Not able to init consul communication", e);
        }
    }

    private class SessionRenewer implements Runnable {

        @Override
        public void run() {
            sessionLock.lock();
            try {
                if (sessionId == null) {
                    String sessionName = SERVICE_NAME.concat("_").concat(instanceId).concat("_session");
                    if (log.isDebugEnabled()) {
                        log.debug("Creating consul session with name <{}> with TTL {} seconds", sessionName, consulSessionTTLInSecond);
                    }
                    com.orbitz.consul.model.session.ImmutableSession.Builder sessionBuilder = ImmutableSession.builder();
                    sessionBuilder = sessionBuilder.name(sessionName);
                    sessionBuilder = sessionBuilder.addChecks("service:" + checkId);
                    sessionBuilder = sessionBuilder.ttl(consulSessionTTLInSecond + "s");
                    sessionBuilder = sessionBuilder.lockDelay(consulLockDelayInSecond + "s");
                    Session session = sessionBuilder.build();
                    sessionId = consul.sessionClient().createSession(session).getId();
                    if (log.isDebugEnabled()) {
                        log.debug("Created consul session with id <{}>", sessionId);
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("Renewing consul session with id <{}>", sessionId);
                    }
                    Optional<SessionInfo> sessionInfo = consul.sessionClient().renewSession(sessionId);
                    if (!sessionInfo.isPresent()) {
                        sessionId = null;
                        // no session renewed
                        log.warn("Not able to renew session");
                        banish();
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Consul session with id <{}> renewed", sessionId);
                    }
                }
            } catch (ConsulException e) {
                sessionId = null;
                log.error("Not able to create or renew session", e);
                banish();
            } finally {
                sessionLock.unlock();
            }
        }

    }

    private class LockAquisition implements Runnable, ConsulResponseCallback<Optional<com.orbitz.consul.model.kv.Value>> {

        AtomicReference<BigInteger> responseIndex = new AtomicReference<BigInteger>(new BigInteger("0"));

        @Override
        public void run() {
            sessionLock.lock();
            try {
                if (sessionId == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session, do nothing");
                        Date retryStartDate = new Date(System.currentTimeMillis() + (healthCheckPeriodInSecond * 1000));
                        consulLockAquisitionTaskScheduler.schedule(this, retryStartDate);
                        return;
                    }
                } else {
                    // first off all, we try to acquire leadership onto resource
                    acquireLeadership();
                }
            } finally {
                sessionLock.unlock();
            }
        }

        private void acquireLeadership() {
            if (log.isDebugEnabled()) {
                log.debug("Trying to acquire lock on resource {}", LEARDER_KEY);
            }
            // the value of the key will be used by consul template to generated nginx config
            String value = instanceIp + ":" + listenPort;
            try {
                if (consul.keyValueClient().acquireLock(LEARDER_KEY, value, sessionId)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Lock is acquired on resource {}, I am the leader", LEARDER_KEY);
                    }
                    elect();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Not able to get lock on resource {}, I am banished", LEARDER_KEY);
                    }
                    banish();
                }
            } catch (ConsulException e) {
                log.error("Not able to acquire leadership due to consul exception", e);
                banish();
            } finally {
                watch();
            }
        }

        private void watch() {
            if (log.isTraceEnabled()) {
                log.trace("Watch: Block read key <{}>", LEARDER_KEY);
            }
            try {
                consul.keyValueClient().getValue(LEARDER_KEY, QueryOptions.blockMinutes(consulQueryTimeoutInMin, responseIndex.get()).build(), this);
            } catch (ConsulException e) {
                log.error("Not able to acquire watch leader resource due to consul exception", e);
                banish();
                Date retryStartDate = new Date(System.currentTimeMillis() + (healthCheckPeriodInSecond * 1000));
                consulLockAquisitionTaskScheduler.schedule(this, retryStartDate);
            }
        }

        @Override
        public void onComplete(ConsulResponse<Optional<com.orbitz.consul.model.kv.Value>> consulResponse) {
            sessionLock.lock();
            if (sessionId == null) {
                if (log.isTraceEnabled()) {
                    log.trace("No known consul session, do nothing");
                }
                // just wait during the health check delay
                Date retryStartDate = new Date(System.currentTimeMillis() + (healthCheckPeriodInSecond * 1000));
                consulLockAquisitionTaskScheduler.schedule(this, retryStartDate);
                return;
            }
            try {
                if (consulResponse.getResponse().isPresent()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Response received");
                    }
                    responseIndex.set(consulResponse.getIndex());
                    com.orbitz.consul.model.kv.Value response = consulResponse.getResponse().get();
                    if (response.getSession().isPresent()) {
                        String leaderSessionId = response.getSession().get();
                        if (sessionId.equals(leaderSessionId)) {
                            if (log.isTraceEnabled()) {
                                log.trace("I am already leader with session <{}>", leaderSessionId);
                            }
                        } else {
                            if (log.isTraceEnabled()) {
                                log.trace("Another instance is already elected with session <{}>", leaderSessionId);
                            }
                            banish();
                        }
                    } else {
                        // no session associated to value, try aqcuiring lock
                        acquireLeadership();
                        return;
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("No response received");
                    }
                    acquireLeadership();
                    return;
                }
                watch();
            } finally {
                sessionLock.unlock();
            }
        }

        @Override
        public void onFailure(Throwable e) {
            log.error("Error occured while watching leader key, will retry later", e);
            // just wait during the health check delay
            Date retryStartDate = new Date(System.currentTimeMillis() + (healthCheckPeriodInSecond * 1000));
            consulLockAquisitionTaskScheduler.schedule(this, retryStartDate);
        }

    }

}
