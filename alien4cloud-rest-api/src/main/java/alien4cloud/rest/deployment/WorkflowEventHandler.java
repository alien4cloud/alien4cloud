package alien4cloud.rest.deployment;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Maps;
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
import alien4cloud.paas.model.PaaSWorkflowMonitorEvent;
import alien4cloud.rest.websocket.ISecuredHandler;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;

@Slf4j
@Component
public class WorkflowEventHandler implements IPaasEventListener<AbstractMonitorEvent>, ISecuredHandler, InitializingBean {

    private static final String TOPIC_PREFIX = "/topic/workflow-events";

    private static final Pattern DESTINATION_PATTERN = Pattern.compile(TOPIC_PREFIX + "/(.*?)(:?/.*)?");

    @Resource
    private IPaasEventService paasEventService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private SimpMessagingTemplate template;

    protected void send(AbstractMonitorEvent event) {
        String eventType = MappingBuilder.indexTypeFromClass(event.getClass());
        String topicName = TOPIC_PREFIX + '/' + event.getDeploymentId() + '/' + eventType;
        if (log.isDebugEnabled()) {
            log.debug("Send [" + event.getClass().getSimpleName() + "] to [" + topicName + "]: " + event);
        }
        template.convertAndSend(topicName, event);
        if (event instanceof PaaSWorkflowMonitorEvent) {
            Deployment deployment = alienDAO.findById(Deployment.class, event.getDeploymentId());

            if (deployment != null) {
                PaaSWorkflowMonitorEvent pwme = (PaaSWorkflowMonitorEvent)event;
                if (log.isDebugEnabled()) {
                    log.debug("Workflow {} started with executionId {} (subkworkflow: {})", pwme.getWorkflowId(), pwme.getExecutionId(), pwme.getSubworkflow());
                }
                String workflowId = pwme.getWorkflowId();
                if (pwme.getSubworkflow() != null) {
                    workflowId = pwme.getSubworkflow();
                }
                updateDeploymentExecutionId(deployment, workflowId, pwme.getExecutionId());
            }
        }
    }

    private void updateDeploymentExecutionId(Deployment deployment, String workflowId, String executionId) {
        if (deployment.getWorkflowExecutions() == null) {
            Map<String, String> workflowExecutions = Maps.newHashMap();
            deployment.setWorkflowExecutions(workflowExecutions);
        }
        String knownExecutionId = deployment.getWorkflowExecutions().get(workflowId);
        if (knownExecutionId == null || !executionId.equals(knownExecutionId)) {
            deployment.getWorkflowExecutions().put(workflowId, executionId);
            alienDAO.save(deployment);
        }
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
            throw new IllegalArgumentException("Cannot handle this destination [" + destination + "]");
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

    @Override
    public void eventHappened(AbstractMonitorEvent event) {
        send(event);
        if (log.isTraceEnabled()) {
            log.trace("Pushed event {} for workflow {}", event, event.getDeploymentId());
        }
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return AbstractPaaSWorkflowMonitorEvent.class.isAssignableFrom(event.getClass());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        paasEventService.addListener(this);
    }
}
