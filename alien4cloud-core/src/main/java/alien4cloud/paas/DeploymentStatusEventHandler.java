package alien4cloud.paas;

import java.util.Date;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * This handler receives Deployment Status events and update the deployment if the status is un-deployed.
 */
@Slf4j
@Service
public class DeploymentStatusEventHandler implements IPaasEventListener<AbstractMonitorEvent> {
    @Inject
    private DeploymentService deploymentService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public void eventHappened(AbstractMonitorEvent aEvent) {
        PaaSDeploymentStatusMonitorEvent event = (PaaSDeploymentStatusMonitorEvent) aEvent;
        log.debug("Received a deployment status event for deployment {} with a new status to {}", event.getDeploymentId(), event.getDeploymentStatus());
        if (DeploymentStatus.UNDEPLOYED.equals(event.getDeploymentStatus())) {
            Deployment deployment = deploymentService.get(event.getDeploymentId());
            if (deployment == null) {
                log.error("No deployment with id {} can be found while processing status event with status update to {}.", event.getDeploymentId(),
                        event.getDeploymentStatus());
                return;
            }
            deployment.setEndDate(new Date(event.getDate()));
            alienDAO.save(deployment);
            log.debug("Deployment {} end date has been updated to {} based on received UNDEPLOYED status event", event.getDeploymentId(),
                    deployment.getEndDate());
        }
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSDeploymentStatusMonitorEvent;
    }
}