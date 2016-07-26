package alien4cloud.rest.health;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import alien4cloud.events.HALeaderElectionEvent;

import com.google.common.base.Optional;
import com.orbitz.consul.Consul;
import com.orbitz.consul.Consul.Builder;
import com.orbitz.consul.async.ConsulResponseCallback;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.Session;
import com.orbitz.consul.model.session.SessionCreatedResponse;
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

    @Value("${ha.consulAgentIp:127.0.0.1}")
    private String consulAgentIp;

    @Value("${ha.consulAgentPort:8500}")
    private int consulAgentPort;

    private String instanceId;

    @Value("${ha.instanceIp:#{null}}")
    private String instanceIp;

    @Value("${ha.healthCheckPeriodInSecond:5}")
    private long healthCheckPeriodInSecond;

    /**
     * When the lock acquisition fail, the delay before retrying to acquire a lock.
     */
    @Value("${ha.lockAcquisitionDelayInSecond:10}")
    private long lockAcquisitionDelayInSecond;

    @Value("${ha.consulSessionTTLInSecond:1800}")
    private long consulSessionTTLInSecond;

    @Value("${ha.consulLockDelayInSecond:15}")
    private long consulLockDelayInSecond;

    @Value("${ha.consulQueryTimeoutInMin:3}")
    private int consulQueryTimeoutInMin;

    @Value("${ha.consulConnectTimeoutInMillis:#{1000 * 30}}" /* default 30 s */)
    private long consulConnectTimeoutInMillis;

    @Value("${ha.consulReadTimeoutInMillis:#{1000 * 60 * 5}}" /* default 5 min */)
    private long consulReadTimeoutInMillis;

    @Value("${ha.consul_tls_enabled:#{false}}")
    private boolean consulTlsEnabled;

    @Value("${ha.keyStorePath:#{null}}")
    private String keyStorePath;

    @Value("${ha.trustStorePath:#{null}}")
    private String trustStorePath;

    @Value("${ha.keyStoresPwd:#{null}}")
    private String keyStoresPwd;

    @Value("${ha.serverProtocol:http}")
    private String serverProtocol;
    
    /**
     * The percentage off session TTL to be used in renew.
     * 0.8 means the session will be renewed at 80%.
     */
    private float renewAsPercentageofSessionTTL = 0.8f;

    @Resource
    private ApplicationContext alienContext;

    private int listenPort;

    private Consul consul;

    private String checkId;

    /** The last known sessionId. */
    private volatile String lastSessionId;

    /** The current session (created or renewed). */
    private SessionInfo session;

    /**
     * Guard for the session.
     */
    private ReentrantLock sessionLock = new ReentrantLock();

    private volatile boolean leader;
    private ReentrantLock leaderLock = new ReentrantLock();

    private ThreadPoolTaskScheduler sessionRenewerTaskScheduler;

    private ThreadPoolTaskScheduler consulLockAquisitionTaskScheduler;

    @Deprecated
    private volatile boolean fail;

    private SessionRenewer sessionRenewer = new SessionRenewer();
    
    private LockAquisition lockAquisition = new LockAquisition();

    @Deprecated
    public void setFail(boolean fail) {
        this.fail = fail;
    }

    @PostConstruct
    public void init() {
        if (!haEnabled) {
            return;
        }

        Builder consulBuilder = Consul.builder();
        String url = String.format("%s://%s:%d", this.consulTlsEnabled ? "https" : "http", consulAgentIp, consulAgentPort);
        consulBuilder.withUrl(url);
        if (log.isDebugEnabled()) {
            log.debug("Will connect to consul using url <{}>, {}ms connect timeout and {}ms read timeout", url, consulConnectTimeoutInMillis,
                    consulReadTimeoutInMillis);
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("Consul connection is secured, initializing the SSL Context using key store <{}> and trust store <{}>", this.keyStorePath,
                        this.trustStorePath);
            }
            SSLContext sslContext = SSLContext.getInstance("TLSv1");

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(new File(this.keyStorePath)), this.keyStoresPwd.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            // FIXME: really necessary ?
            kmf.init(ks, this.keyStoresPwd.toCharArray());

            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(new FileInputStream(new File(this.trustStorePath)), this.keyStoresPwd.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            consulBuilder = consulBuilder.withSslContext(sslContext);
        } catch (KeyStoreException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException e) {
            log.error("Not able to initialize SSL Context", e);
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
        if (session != null) {
            try {
                consul.sessionClient().destroySession(session.getId());
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

        // initialize the check
        com.orbitz.consul.model.agent.ImmutableRegistration.Builder builder = ImmutableRegistration.builder();
        builder.address(this.instanceIp);
        builder.port(listenPort);
        builder.name(SERVICE_NAME);
        builder.id(this.checkId);
        builder.check(Registration.RegCheck.http(getCheckUrl().toExternalForm(), healthCheckPeriodInSecond));
        ImmutableRegistration registration = builder.build();
        consul.agentClient().register(registration);

        // we start the session renewer (that will also create the session after 2 health checks)
        // we need to wait here before creating a session otherwise consul will reject us
        scheduleSessionRenewInSeconds(2 * healthCheckPeriodInSecond);
    }

    private URL getCheckUrl() throws MalformedURLException {
        return new URL(serverProtocol + "://" + instanceIp + ":" + listenPort + "/rest/latest/health/check");
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
            if (fail) {
                scheduleSessionRenewInSeconds(consulSessionTTLInSecond);
                return;
            }
            sessionLock.lock();
            try {
                boolean sessionWasNull = (session == null);
                if (lastSessionId == null) {
                    session = acquireSession();
                } else {
                    session = renewSession(lastSessionId);
                }
                if (session != null) {
                    lastSessionId = session.getId();
                    long ttlInSeconde = Math.max(Math.round(consulSessionTTLInSecond * 1000 * renewAsPercentageofSessionTTL) / 1000, 1);
                    Optional<String> ttlAsString = session.getTtl();
                    if (ttlAsString.isPresent()) {
                        try {
                            if (log.isTraceEnabled()) {
                                log.trace("TTL value received: {}", ttlAsString.get());
                            }
                            ttlInSeconde = Long.parseLong(ttlAsString.get().substring(0, ttlAsString.get().length() - 1));
                            // renew the session at 80% of its TTL, but never less than 1 seconde
                            ttlInSeconde = Math.max(Math.round(ttlInSeconde * 1000 * renewAsPercentageofSessionTTL) / 1000, 1);
                            if (log.isTraceEnabled()) {
                                log.trace("Returned TTL is <{}>, so the session will be renewed in {}s", ttlAsString.get(), ttlInSeconde);
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                    scheduleSessionRenewInSeconds(ttlInSeconde);
                    if (sessionWasNull) {
                        // No current session, launch the lock acquisition task
                        scheduleLockAcquisitionInSeconds(0);
                    }
                } else {
                    // session is null
                    if (log.isDebugEnabled()) {
                        log.debug("No active session on Consul, something went wrong");
                    }
                    // the session can not be renewed so we need to retry quickly
                    // 2 health check period sounds good !
                    scheduleSessionRenewInSeconds(2 * healthCheckPeriodInSecond);
                    banish();
                }
            } finally {
                sessionLock.unlock();
            }
        }

    }

    private void scheduleSessionRenewInSeconds(long secondes) {
        if (log.isTraceEnabled()) {
            log.trace("Scheduling session renew task in {}s", secondes);
        }
        Date sessionRenewStartDate = new Date(System.currentTimeMillis() + (secondes * 1000));
        sessionRenewerTaskScheduler.schedule(sessionRenewer, sessionRenewStartDate);
    }

    private void scheduleLockAcquisitionInSeconds(long secondes) {
        Date startDate = new Date(System.currentTimeMillis() + (secondes * 1000));
        if (log.isTraceEnabled()) {
            log.trace("Scheduling lock acquisition task in {}s", secondes);
        }
        consulLockAquisitionTaskScheduler.schedule(lockAquisition, startDate);
    }

    private SessionInfo renewSession(String sessionId) {
        try {
            // first of all just verify the session exists on Consul
            Optional<SessionInfo> sessionInfo = consul.sessionClient().getSessionInfo(sessionId);
            if (sessionInfo.isPresent()) {
                // if the session exist, then renew it
                if (log.isTraceEnabled()) {
                    log.trace("Renewing session with id <{}>", sessionId);
                }
                sessionInfo = consul.sessionClient().renewSession(sessionId);
                if (sessionInfo.isPresent()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Consul session with id <{}> renewed", sessionId);
                    }
                    return sessionInfo.get();
                } else {
                    log.warn("Not able to renew session");
                    return null;
                }
            } else {
                // the session doesn't exist anymore, creating a new one
                if (log.isTraceEnabled()) {
                    log.trace("Session with id <{}> not found, will create a new one", sessionId);
                }
                return acquireSession();
            }
        } catch (Exception e) {
            log.error("Not able to renew session", e);
            return null;
        }
    }

    private SessionInfo acquireSession() {
        try {
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
            SessionCreatedResponse response = consul.sessionClient().createSession(session);
            Optional<SessionInfo> sessionInfo = consul.sessionClient().getSessionInfo(response.getId());
            if (sessionInfo.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("Created consul session with id <{}>", sessionInfo.get().getId());
                }
                return sessionInfo.get();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Not able to create session", e);
            return null;
        }
    }

    private class LockAquisition implements Runnable, ConsulResponseCallback<Optional<com.orbitz.consul.model.kv.Value>> {

        AtomicReference<BigInteger> responseIndex = new AtomicReference<BigInteger>(new BigInteger("0"));

        @Override
        public void run() {
            if (log.isTraceEnabled()) {
                log.trace("LockAquisition task run");
            }
            sessionLock.lock();
            try {
                if (session == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session when running LockAquisition task, do nothing");
                    }
                    return;
                } else {
                    // Try to acquire leadership onto resource
                    acquireLeadership();
                }
            } finally {
                sessionLock.unlock();
            }
        }

        private void acquireLeadership() {
            sessionLock.lock();
            try {
                if (session == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session, do nothing");
                    }
                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Trying to acquire lock on resource {}", LEARDER_KEY);
                }
                // the value of the key will be used by consul template to generated nginx config
                String value = instanceIp + ":" + listenPort;
                if (consul.keyValueClient().acquireLock(LEARDER_KEY, value, session.getId())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Lock is acquired on resource {}, I am the leader", LEARDER_KEY);
                    }
                    elect();
                    watch();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Not able to get lock on resource {}, I am banished", LEARDER_KEY);
                    }
                    banish();
                    watch();
                }
            } catch (Exception e) {
                log.error("Not able to acquire leadership due to consul exception", e);
                if (session != null) {
                    scheduleLockAcquisitionInSeconds(lockAcquisitionDelayInSecond);
                    return;
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session while exception occured when trying to acquire leadership, do nothing");
                    }
                }
                return;
            } finally {
                sessionLock.unlock();
            }
        }

        private void watch() {
            sessionLock.lock();
            try {
                if (session == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session while trying to watch changes on the key, do nothing");
                    }
                    return;
                }
                if (log.isTraceEnabled()) {
                    log.trace("Watch: Block read key <{}>", LEARDER_KEY);
                }
                consul.keyValueClient().getValue(LEARDER_KEY, QueryOptions.blockMinutes(consulQueryTimeoutInMin, responseIndex.get()).build(), this);
            } catch (Exception e) {
                log.error("Not able to watch leader resource due to consul exception", e);
                if (session != null) {
                    scheduleLockAcquisitionInSeconds(lockAcquisitionDelayInSecond);
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session while exception occurred when trying to watch changes on the key, do nothing");
                    }
                }
            } finally {
                sessionLock.unlock();
            }
        }

        @Override
        public void onComplete(ConsulResponse<Optional<com.orbitz.consul.model.kv.Value>> consulResponse) {
            sessionLock.lock();
            try {
                if (session == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session when watch key returns, do nothing");
                    }
                    return;
                }
                if (consulResponse.getResponse().isPresent()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Response received in onComplete");
                    }
                    responseIndex.set(consulResponse.getIndex());
                    com.orbitz.consul.model.kv.Value response = consulResponse.getResponse().get();
                    if (response.getSession().isPresent()) {
                        String leaderSessionId = response.getSession().get();
                        if (session.getId().equals(leaderSessionId)) {
                            if (log.isTraceEnabled()) {
                                log.trace("I am already leader with session <{}>, just watch again the key for changes", leaderSessionId);
                            }
                            watch();
                            return;
                        } else {
                            if (log.isTraceEnabled()) {
                                log.trace("Another instance is already elected with session <{}>, just watch again the key for changes", leaderSessionId);
                            }
                            watch();
                            return;
                        }
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace("No session associated to the key, will try to acquire lock");
                        }
                        acquireLeadership();
                        return;
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("No response received, will try to acquire lock");
                    }
                    acquireLeadership();
                    return;
                }
            } catch (Exception e) {
                log.error("An exception occured in onComplete", e);
                if (session != null) {
                    scheduleLockAcquisitionInSeconds(lockAcquisitionDelayInSecond);
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session on exception thrown in onComplete, do nothing");
                    }
                }
            } finally {
                sessionLock.unlock();
            }
        }

        @Override
        public void onFailure(Throwable e) {
            log.error("Error occured while watching leader key", e);
            sessionLock.lock();
            try {
                if (session != null) {
                    scheduleLockAcquisitionInSeconds(lockAcquisitionDelayInSecond);
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("No known consul session while exception thrown in ConsulResponseCallback, do nothing");
                    }
                }
            } finally {
                sessionLock.unlock();
            }
        }

    }

}
