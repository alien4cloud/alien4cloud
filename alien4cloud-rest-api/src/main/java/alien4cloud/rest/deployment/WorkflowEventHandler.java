package alien4cloud.rest.deployment;

import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.runtime.*;
import alien4cloud.paas.model.*;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.lang3.StringUtils;
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
import alien4cloud.rest.websocket.ISecuredHandler;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;

@Slf4j
@Component
public class WorkflowEventHandler implements IPaasEventListener<AbstractMonitorEvent>, ISecuredHandler, InitializingBean {

    private static final String TOPIC_PREFIX = "/topic/deployment-events";

    private static final Pattern DESTINATION_PATTERN = Pattern.compile(TOPIC_PREFIX + "/(.*?)(:?/.*)?");

    @Resource
    private IPaasEventService paasEventService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private SimpMessagingTemplate template;

    protected void send(AbstractMonitorEvent event) {
        String eventType = "paasworkflowmonitorevent";
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
                enrichExecution(deployment, pwme);
                String workflowId = pwme.getWorkflowId();
                if (pwme.getSubworkflow() != null) {
                    workflowId = pwme.getSubworkflow();
                }
                updateDeploymentExecutionId(deployment, workflowId, pwme.getExecutionId());
            }
        } else if (event instanceof PaaSWorkflowStartedEvent) {
            createExecution((PaaSWorkflowStartedEvent)event);
        } else if (event instanceof PaaSWorkflowSucceededEvent) {
            updateExecution((PaaSWorkflowSucceededEvent)event, ExecutionStatus.SUCCEEDED);
        } else if (event instanceof PaaSWorkflowFailedEvent) {
            updateExecution((PaaSWorkflowFailedEvent)event, ExecutionStatus.FAILED);
        } else if (event instanceof PaaSWorkflowCancelledEvent) {
            updateExecution((PaaSWorkflowCancelledEvent)event, ExecutionStatus.CANCELLED);
        } else if (event instanceof TaskSentEvent) {
            log.info("TaskSentEvent recceived for #" + ((TaskSentEvent)event).getTaskId());
            createTask((TaskSentEvent)event);
        } else if (event instanceof TaskStartedEvent) {
            log.info("TaskStartedEvent recceived for #" + ((TaskStartedEvent)event).getTaskId());
            TaskStartedEvent taskStartedEvent = (TaskStartedEvent)event;
            updateTask(taskStartedEvent.getTaskId(), TaskStatus.STARTED, taskStartedEvent, null);
        } else if (event instanceof TaskSucceededEvent) {
            log.info("TaskSucceededEvent recceived for #" + ((TaskSucceededEvent)event).getTaskId());
            TaskSucceededEvent taskEvent = (TaskSucceededEvent)event;
            updateTask(taskEvent.getTaskId(), TaskStatus.SUCCEEDED, taskEvent, null);
        } else if (event instanceof TaskFailedEvent) {
            log.info("TaskFailedEvent recceived for #" + ((TaskFailedEvent)event).getTaskId());
            TaskFailedEvent taskEvent = (TaskFailedEvent)event;
            updateTask(taskEvent.getTaskId(), TaskStatus.FAILED, taskEvent, taskEvent.getErrorCauses());
        } else if (event instanceof TaskCancelledEvent) {
            log.info("TaskFailedEvent recceived for #" + ((TaskCancelledEvent)event).getTaskId());
            TaskCancelledEvent taskEvent = (TaskCancelledEvent)event;
            updateTask(taskEvent.getTaskId(), TaskStatus.CANCELLED, taskEvent, null);
        } else if (event instanceof WorkflowStepStartedEvent) {
            log.info("WorkflowStepStartedEvent recceived for #" + ((WorkflowStepStartedEvent)event).getStepId());
            createWorkflowStepInstance((WorkflowStepStartedEvent)event);
        } else if (event instanceof WorkflowStepCompletedEvent) {
            log.info("WorkflowStepCompletedEvent recceived for #" + ((WorkflowStepCompletedEvent)event).getStepId());
            updateWorkflowStepInstance((WorkflowStepCompletedEvent)event);
        }
    }

    // TODO: move elsewhere since this has nothing to do in REST module
    private void createTask(TaskSentEvent e) {

        String stepInstanceId = getWorkflowStepInstanceIdForTask(e);

        Task t = new Task();
        t.setId(e.getTaskId());
        t.setExecutionId(e.getExecutionId());
        t.setNodeId(e.getNodeId());
        t.setInstanceId(e.getInstanceId());
        t.setOperationName(e.getOperationName());
        t.setDeploymentId(e.getDeploymentId());
        t.setWorkflowStepInstanceId(stepInstanceId);
        t.setScheduleDate(new Date(e.getDate()));
        t.setStatus(TaskStatus.SCHEDULED);
        alienDAO.save(t);
    }

    private String getWorkflowStepInstanceIdForTask(AbstractTaskEvent taskEvent) {
        // find the WorkflowStepInstance that has the same executionId, operationName, nodeId, instanceId
        String stepInstanceId = null;
        Map<String, String[]> filter = Maps.newHashMap();
        filter.put("executionId", new String[] { taskEvent.getExecutionId() });
        if (taskEvent.getWorkflowStepId() == null) {
            filter.put("nodeId", new String[] { taskEvent.getNodeId() });
            filter.put("instanceId", new String[] { taskEvent.getInstanceId() });
            filter.put("targetNodeId", new String[] { taskEvent.getTargetNodeId() });
            filter.put("targetInstanceId", new String[] { taskEvent.getTargetInstanceId() });
            filter.put("operationName", new String[] { taskEvent.getOperationName() });
        } else {
            filter.put("stepId", new String[] { taskEvent.getWorkflowStepId() });
            filter.put("instanceId", new String[] { taskEvent.getInstanceId() });
            filter.put("targetInstanceId", new String[] { taskEvent.getTargetInstanceId() });
        }
        GetMultipleDataResult<WorkflowStepInstance> result = alienDAO.find(WorkflowStepInstance.class, filter, Integer.MAX_VALUE);
        if (result.getTotalResults() > 0) {
            stepInstanceId = result.getData()[0].getId();
        }
        return stepInstanceId;
    }

    private void createWorkflowStepInstance(WorkflowStepStartedEvent e) {
        WorkflowStepInstance i = new WorkflowStepInstance();
        i.setStepId(e.getStepId());
        i.setOperationName(e.getOperationName());
        i.setDeploymentId(e.getDeploymentId());
        i.setExecutionId(e.getExecutionId());
        i.setId(UUID.randomUUID().toString());
        i.setNodeId(e.getNodeId());
        i.setInstanceId(e.getInstanceId());
        i.setTargetNodeId(e.getTargetNodeId());
        i.setTargetInstanceId(e.getTargetInstanceId());
        i.setStatus(WorkflowStepInstanceStatus.STARTED);
        alienDAO.save(i);
    }

    private void updateWorkflowStepInstance(WorkflowStepCompletedEvent e) {
        Map<String, String[]> filter = Maps.newHashMap();
        filter.put("executionId", new String[] { e.getExecutionId() });
        filter.put("deploymentId", new String[] { e.getDeploymentId() });
        filter.put("stepId", new String[] { e.getStepId() });
        filter.put("nodeId", new String[] { e.getNodeId() });
        filter.put("instanceId", new String[] { e.getInstanceId() });
        filter.put("targetNodeId", new String[] { e.getTargetNodeId() });
        filter.put("targetInstanceId", new String[] { e.getTargetInstanceId() });

        GetMultipleDataResult<WorkflowStepInstance> result = alienDAO.find(WorkflowStepInstance.class, filter, 1);
        if (result.getTotalResults() > 0) {
            WorkflowStepInstance i = result.getData()[0];
            i.setStatus(WorkflowStepInstanceStatus.COMPLETED);
            alienDAO.save(i);
        }
    }

    // TODO: move elsewhere since this has nothing to do in REST module
    private void updateTask(String id, TaskStatus status, AbstractTaskEvent taskEvent, String details) {
        Task t = alienDAO.findById(Task.class, id);
        if (t != null) {
            t.setStatus(status);

            if (t.getWorkflowStepInstanceId() == null) {
                String stepInstanceId = getWorkflowStepInstanceIdForTask(taskEvent);
                t.setWorkflowStepInstanceId(stepInstanceId);
            }

            if (org.springframework.util.StringUtils.hasText(details)) {
                t.setDetails(details);
            }
            alienDAO.save(t);

            if (TaskStatus.FAILED == status) {
                // we must update the related execution
                if (t.getExecutionId() != null) {
                    Execution e = alienDAO.findById(Execution.class, t.getExecutionId());
                    if (e != null && !e.isHasFailedTasks()) {
                        e.setHasFailedTasks(true);
                        alienDAO.save(e);
                    }
                }
                // we must update the related step instance
                if (t.getWorkflowStepInstanceId() != null) {
                    WorkflowStepInstance i = alienDAO.findById(WorkflowStepInstance.class, t.getWorkflowStepInstanceId());
                    if (i != null && !i.isHasFailedTasks()) {
                        i.setHasFailedTasks(true);
                        alienDAO.save(i);
                    }
                }
            }
        }
    }

    // TODO: move elsewhere since this has nothing to do in REST module
    private void updateExecution(PaaSWorkflowFinishedEvent e, ExecutionStatus s) {
        Execution execution = alienDAO.findById(Execution.class, e.getExecutionId());
        if (execution != null) {
            execution.setStatus(s);
            execution.setEndDate(new Date(e.getDate()));
            alienDAO.save(execution);
        }
    }

    // TODO: move elsewhere since this has nothing to do in REST module
    private void createExecution(PaaSWorkflowStartedEvent e) {
        Execution execution = new Execution();
        execution.setDeploymentId(e.getDeploymentId());
        execution.setId(e.getExecutionId());
        execution.setWorkflowId(e.getWorkflowId());
        execution.setWorkflowName(e.getWorkflowName());
        execution.setDisplayWorkflowName(e.getWorkflowName());
        execution.setStartDate(new Date(e.getDate()));
        execution.setStatus(ExecutionStatus.RUNNING);
        alienDAO.save(execution);
    }

    // TODO: move elsewhere since this has nothing to do in REST module
    private void enrichExecution(Deployment deployment, PaaSWorkflowMonitorEvent e) {
        if (!StringUtils.isEmpty(e.getSubworkflow())) {
            Execution execution = alienDAO.findById(Execution.class, e.getExecutionId());
            if (execution != null) {
                execution.setDisplayWorkflowName(e.getSubworkflow());
                alienDAO.save(execution);
            }
        }
    }

    // TODO: move elsewhere since this has nothing to do in REST module
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
