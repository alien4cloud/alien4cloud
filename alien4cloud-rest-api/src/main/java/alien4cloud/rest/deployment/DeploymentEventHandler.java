package alien4cloud.rest.deployment;

import java.security.Principal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.IPaasEventListener;
import alien4cloud.paas.IPaasEventService;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.AbstractPaaSWorkflowMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.rest.websocket.ISecuredHandler;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;

@Slf4j
@Component
public class DeploymentEventHandler implements IPaasEventListener<AbstractMonitorEvent>, ISecuredHandler, InitializingBean {

    private static final String TOPIC_PREFIX = "/topic/deployment-events";
    private static final String ENV_TOPIC_PREFIX = "/topic/environment-events";

    private static final Pattern DESTINATION_PATTERN = Pattern.compile(TOPIC_PREFIX + "/(.*?)(:?/.*)?");
    private static final Pattern ENV_DESTINATION_PATTERN = Pattern.compile(ENV_TOPIC_PREFIX + "/(.*?)(:?/.*)?");

    @Resource
    private IPaasEventService paasEventService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private SimpMessagingTemplate template;

    protected void send(AbstractMonitorEvent event) {
        String eventType = MappingBuilder.indexTypeFromClass(event.getClass());
        String topicName = TOPIC_PREFIX + '/' + event.getDeploymentId() + '/' + eventType;
        dispatchEvent(event, topicName);

        if (event instanceof PaaSDeploymentStatusMonitorEvent) {
            Deployment deployment = alienDAO.findById(Deployment.class, event.getDeploymentId());
            if (deployment != null && deployment.getEnvironmentId() != null) {
                // dispatch an event on the environment topic
                topicName = ENV_TOPIC_PREFIX + "/" + deployment.getEnvironmentId();
                dispatchEvent(event, topicName);
            }
        }
    }

    private void dispatchEvent(AbstractMonitorEvent event, String topicName) {
        log.debug("Send [{}] to [{}]: {}", event.getClass().getSimpleName(), topicName, event);
        template.convertAndSend(topicName, event);
    }

    /**
     * Check if the destination path can be handled by this event handler
     *
     * @param destination the destination
     * @return true if the event handler manage the destination
     */
    @Override
    public boolean canHandleDestination(String destination) {
        Matcher matcher = DESTINATION_PATTERN.matcher(destination);
        return matcher.matches();
    }

    /**
     * Check if the current user is authorized for this destination
     *
     * @param destination the destination
     */
    @Override
    public void checkAuthorization(Principal user, String destination) {
        Matcher matcher = DESTINATION_PATTERN.matcher(destination);
        Authentication authentication = (Authentication) user;
        User a4cUser = (User) authentication.getPrincipal();
        if (matcher.matches()) {
            String deploymentId = matcher.group(1);
            checkDeploymentAuthorization(authentication, a4cUser, deploymentId);
        } else {
            matcher = ENV_DESTINATION_PATTERN.matcher(destination);
            if (matcher.matches()) {
                String environmentId = matcher.group(1);
                checkEnvironmentAuthorization(a4cUser, environmentId);
            } else {
                throw new IllegalArgumentException("Cannot handle this destination [" + destination + "]");
            }
        }
    }

    private void checkDeploymentAuthorization(Authentication authentication, User a4cUser, String deploymentId) {
        Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
        switch (deployment.getSourceType()) {
        case APPLICATION:
            // check if the user has right for the environment associated with the deployment.
            ApplicationEnvironment environment = alienDAO.findById(ApplicationEnvironment.class, deployment.getEnvironmentId());
            if (environment == null) {
                log.error("Environment with id [{}] do not exist any more for deployment [{}]", deployment.getEnvironmentId(), deployment.getId());
                throw new NotFoundException(
                        "Environment with id [" + deployment.getEnvironmentId() + "] do not exist any more for deployment [" + deployment.getId() + "]");
            }
            AuthorizationUtil.checkAuthorization(a4cUser, environment, ApplicationRole.APPLICATION_MANAGER, ApplicationEnvironmentRole.values());
            break;
        case CSAR:
            AuthorizationUtil.checkHasOneRoleIn(authentication, Role.COMPONENTS_MANAGER);
        }
    }

    private void checkEnvironmentAuthorization(User a4cUser, String environmentId) {
        ApplicationEnvironment environment = alienDAO.findById(ApplicationEnvironment.class, environmentId);
        if (environment == null) {
            log.error("Environment with id [{}] do not exist any more", environmentId);
            throw new NotFoundException("Environment with id [" + environmentId + "] do not exist any more");
        }
        AuthorizationUtil.checkAuthorization(a4cUser, environment, ApplicationRole.APPLICATION_MANAGER, ApplicationEnvironmentRole.values());
    }

    @Override
    public void eventHappened(AbstractMonitorEvent event) {
        send(event);
        if (log.isTraceEnabled()) {
            log.trace("Pushed event {} for deployment {}", event, event.getDeploymentId());
        }
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return AbstractMonitorEvent.class.isAssignableFrom(event.getClass()) && !AbstractPaaSWorkflowMonitorEvent.class.isAssignableFrom(event.getClass());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        paasEventService.addListener(this);
    }
}
