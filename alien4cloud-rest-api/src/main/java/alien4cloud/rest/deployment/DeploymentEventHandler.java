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

import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.IPaasEventListener;
import alien4cloud.paas.IPaasEventService;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.rest.topology.TopologyService;
import alien4cloud.rest.websocket.ISecuredHandler;
import alien4cloud.security.ApplicationEnvironmentRole;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.Role;
import alien4cloud.security.User;
import alien4cloud.topology.TopologyServiceCore;

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
    private TopologyServiceCore topoServiceCore;
    @Resource
    private TopologyService topoServiceRest;

    @Resource
    private DeploymentService deploymentService;

    @Resource
    private SimpMessagingTemplate template;

    protected void send(AbstractMonitorEvent event) {
        String eventType = MappingBuilder.indexTypeFromClass(event.getClass());
        String topicName = TOPIC_PREFIX + '/' + event.getDeploymentId() + '/' + eventType;
        if (log.isDebugEnabled()) {
            log.debug("Send [" + event.getClass().getSimpleName() + "] to [" + topicName + "]");
        }
        template.convertAndSend(topicName, event);

        if (event instanceof PaaSDeploymentStatusMonitorEvent) {
            Deployment deployment = deploymentService.getDeploymentByPaaSId(event.getDeploymentId());

            if (deployment != null) {
                updateDeploymentStatus(deployment, ((PaaSDeploymentStatusMonitorEvent) event).getDeploymentStatus());

                if (deployment.getDeploymentSetup() != null && deployment.getDeploymentSetup().getEnvironmentId() != null) {
                    // dispatch an event on the environment topic
                    topicName = ENV_TOPIC_PREFIX + "/" + deployment.getDeploymentSetup().getEnvironmentId();
                    template.convertAndSend(topicName, event);
                }
            }
        }
    }

    private void updateDeploymentStatus(Deployment deployment, DeploymentStatus newStatus) {
        if (deployment.getDeploymentStatus() != null && deployment.getDeploymentStatus().equals(newStatus)) {
            return;
        }
        switch (newStatus) {
        case DEPLOYMENT_IN_PROGRESS:
            if (DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS.equals(deployment.getDeploymentStatus())
                    || DeploymentStatus.UNDEPLOYED.equals(deployment.getDeploymentStatus())) {
                log.warn(
                        "Received DEPLOYMENT_IN_PROGRESS status for deployment <{}> for resource <{}> id <{}> of type <{}> while current status is {}. Status will switch to unknown.",
                        deployment.getId(), deployment.getSourceName(), deployment.getSourceId(), deployment.getSourceType(), deployment.getDeploymentStatus());
                newStatus = DeploymentStatus.UNKNOWN;
                return;
            }
            break;
        case DEPLOYED:
            if (DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS.equals(deployment.getDeploymentStatus())
                    || DeploymentStatus.UNDEPLOYED.equals(deployment.getDeploymentStatus())) {
                log.warn(
                        "Received DEPLOYED status for deployment <{}> for resource <{}> id <{}> of type <{}> while current status is {}. Status will switch to unknown.",
                        deployment.getId(), deployment.getSourceName(), deployment.getSourceId(), deployment.getSourceType(), deployment.getDeploymentStatus());
                newStatus = DeploymentStatus.UNKNOWN;
                return;
            }
            break;
        case WARNING:
            if (DeploymentStatus.UNDEPLOYED.equals(deployment.getDeploymentStatus())) {
                log.warn(
                        "Received WARNING status for deployment <{}> for resource <{}> id <{}> of type <{}> while current status is {}. Status will switch to unknown.",
                        deployment.getId(), deployment.getSourceName(), deployment.getSourceId(), deployment.getSourceType(), deployment.getDeploymentStatus());
                newStatus = DeploymentStatus.UNKNOWN;
                return;
            }
            break;
        case FAILURE:
            if (DeploymentStatus.UNDEPLOYED.equals(deployment.getDeploymentStatus())) {
                log.warn(
                        "Received WARNING status for deployment <{}> for resource <{}> id <{}> of type <{}> while current status is {}. Status will switch to unknown.",
                        deployment.getId(), deployment.getSourceName(), deployment.getSourceId(), deployment.getSourceType(), deployment.getDeploymentStatus());
                newStatus = DeploymentStatus.UNKNOWN;
                return;
            }
            break;
        case UNDEPLOYMENT_IN_PROGRESS:
            if (DeploymentStatus.UNDEPLOYED.equals(deployment.getDeploymentStatus())) {
                log.warn(
                        "Received WARNING status for deployment <{}> for resource <{}> id <{}> of type <{}> while current status is {}. Status will switch to unknown.",
                        deployment.getId(), deployment.getSourceName(), deployment.getSourceId(), deployment.getSourceType(), deployment.getDeploymentStatus());
                newStatus = DeploymentStatus.UNKNOWN;
                return;
            }
            break;
        case UNDEPLOYED:
            break;
        case UNKNOWN:
            break;
        default:
            log.error("Received an unexpected status for deployment <{}> for resource <{}> id <{}> of type <{}>: ", deployment.getId(),
                    deployment.getSourceName(), deployment.getSourceId(), deployment.getSourceType(), newStatus);
            return;
        }
        if (DeploymentStatus.UNDEPLOYED.equals(newStatus)) {
            deployment.setEndDate(new Date());
        }
        deployment.setDeploymentStatus(newStatus);
        alienDAO.save(deployment);
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
            ApplicationEnvironment environment = alienDAO.findById(ApplicationEnvironment.class, deployment.getDeploymentSetup().getEnvironmentId());
            if (environment == null) {
                log.error("Environment with id [{}] do not exist any more for deployment [{}]", deployment.getDeploymentSetup().getEnvironmentId(),
                        deployment.getId());
                throw new NotFoundException("Environment with id [" + deployment.getDeploymentSetup().getEnvironmentId()
                        + "] do not exist any more for deployment [" + deployment.getId() + "]");
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
        if (log.isDebugEnabled()) {
            log.debug("Pushed event {} for deployment {}", event, event.getDeploymentId());
        }
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return AbstractMonitorEvent.class.isAssignableFrom(event.getClass());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        paasEventService.addListener(this);
    }
}
