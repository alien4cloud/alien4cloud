package alien4cloud.rest.deployment;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.IPaasEventListener;
import alien4cloud.paas.IPaasEventService;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStorageMonitorEvent;
import alien4cloud.rest.topology.TopologyService;
import alien4cloud.rest.websocket.ISecuredHandler;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.Role;
import alien4cloud.security.User;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.container.model.NormativeBlockStorageConstants;
import alien4cloud.tosca.container.model.topology.NodeTemplate;
import alien4cloud.tosca.container.model.topology.Topology;

@Slf4j
@Component
public class DeploymentEventHandler implements IPaasEventListener<AbstractMonitorEvent>, ISecuredHandler, InitializingBean {

    private static final String TOPIC_PREFIX = "/topic/deployment-events";

    private static final Pattern DESTINATION_PATTERN = Pattern.compile(TOPIC_PREFIX + "/(.*?)(:?/.*)?");

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
            Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
            switch (deployment.getSourceType()) {
            case APPLICATION:
                Application application = alienDAO.findById(Application.class, deployment.getSourceId());
                if (application == null) {
                    log.error("Application with id [{}] do not exist any more for deployment [{}]", deployment.getSourceId(), deployment.getId());
                    throw new NotFoundException("Application with id [" + deployment.getSourceId() + "] do not exist any more for deployment ["
                            + deployment.getId() + "]");
                }
                AuthorizationUtil.checkAuthorization(a4cUser, application, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.values());
                break;
            case CSAR:
                AuthorizationUtil.checkHasOneRoleIn(authentication, Role.COMPONENTS_MANAGER);
            }
        } else {
            throw new IllegalArgumentException("Cannot handle this destination [" + destination + "]");
        }
    }

    @Override
    public void eventHappened(AbstractMonitorEvent event) {
        checkAndProcessBlockStorageEvent(event);
        send(event);
        if (log.isDebugEnabled()) {
            log.debug("Pushed event {} for deployment {}", event, event.getDeploymentId());
        }
    }

    private void checkAndProcessBlockStorageEvent(AbstractMonitorEvent event) {
        if (event instanceof PaaSInstanceStorageMonitorEvent) {
            PaaSInstanceStorageMonitorEvent storageEvent = (PaaSInstanceStorageMonitorEvent) event;
            Deployment depoyment = deploymentService.getDeployment(storageEvent.getDeploymentId());
            Topology topology = topoServiceCore.getMandatoryTopology(depoyment.getTopologyId());
            NodeTemplate nodeTemplate;
            try {
                nodeTemplate = topoServiceCore.getNodeTemplate(topology, storageEvent.getNodeTemplateId());
            } catch (NotFoundException e) {
                log.warn(e.getMessage());
                return;
            }
            String volumeIds = nodeTemplate.getProperties().get(NormativeBlockStorageConstants.VOLUME_ID);
            if (StringUtils.isNotBlank(storageEvent.getVolumeId())) {
                String separator = ",";
                volumeIds = StringUtils.isBlank(volumeIds) ? storageEvent.getVolumeId() : volumeIds.concat(separator).concat(storageEvent.getVolumeId());
            }
            nodeTemplate.getProperties().put(NormativeBlockStorageConstants.VOLUME_ID, volumeIds);
            log.info("Updated topology <{}> to add VolumeId <{}> in . New value is <{}>", topology.getId(), storageEvent.getVolumeId(), volumeIds);
            alienDAO.save(topology);
        }
    }

    @Override
    public Class<AbstractMonitorEvent> getEventType() {
        return AbstractMonitorEvent.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        paasEventService.addListener(this);
    }
}
