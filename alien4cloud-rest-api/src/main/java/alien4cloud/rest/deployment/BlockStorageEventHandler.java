package alien4cloud.rest.deployment;

import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.application.DeploymentSetupService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStorageMonitorEvent;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.NormativeBlockStorageConstants;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import com.google.common.collect.Sets;

@Slf4j
@Component
public class BlockStorageEventHandler extends DeploymentEventHandler {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TopologyServiceCore topoServiceCore;
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private DeploymentSetupService deploymentSetupService;

    @Override
    public void eventHappened(AbstractMonitorEvent event) {
        checkAndProcessBlockStorageEvent((PaaSInstanceStorageMonitorEvent) event);
    }

    private void checkAndProcessBlockStorageEvent(PaaSInstanceStorageMonitorEvent storageEvent) {
        if (StringUtils.isBlank(storageEvent.getVolumeId())) {
            return;
        }

        Deployment deployment = deploymentService.getDeployment(storageEvent.getDeploymentId());
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deployment.getDeploymentSetup().getEnvironmentId());
        ApplicationVersion applicationVersion = applicationVersionService.getOrFail(applicationEnvironment.getCurrentVersionId());
        Topology topology = topoServiceCore.getMandatoryTopology(applicationVersion.getTopologyId());
        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = topoServiceCore.getNodeTemplate(topology, storageEvent.getNodeTemplateId());
        } catch (NotFoundException e) {
            log.warn("Fail to update volumeIds for node " + storageEvent.getNodeTemplateId(), e);
            return;
        }

        if (storageEvent.isDeletable()) {
            log.info("Blockstorage <{}.{}> is a deletable type. Skipping topology volumeId update...", topology.getId(), nodeTemplate.getName());
            return;
        }

        AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(NormativeBlockStorageConstants.VOLUME_ID);
        if (abstractPropertyValue == null) { // the value is set in the topology
            updateNodeTemplate(topology, nodeTemplate, storageEvent, storageEvent.getVolumeId());
        } else if (abstractPropertyValue instanceof ScalarPropertyValue) { // the value is set in the topology
            String volumeIds = ((ScalarPropertyValue) abstractPropertyValue).getValue();
            volumeIds = getAggregatedVolumeIds(volumeIds, storageEvent);
            if (volumeIds == null) {
                return;
            }
            updateNodeTemplate(topology, nodeTemplate, storageEvent, volumeIds);
        } else {
            FunctionPropertyValue function = (FunctionPropertyValue) abstractPropertyValue;
            if (function.getFunction().equals(ToscaFunctionConstants.GET_INPUT)) {
                // the value is set in the input (deployment setup)
                DeploymentSetup deploymentSetup = deploymentSetupService.get(applicationVersion, applicationEnvironment);
                String volumeIds = deploymentSetup.getInputProperties().get(function.getTemplateName());
                volumeIds = getAggregatedVolumeIds(volumeIds, storageEvent);
                if (volumeIds == null) {
                    return;
                }
                deploymentSetup.getInputProperties().put(function.getTemplateName(), volumeIds);
                alienDAO.save(deploymentSetup);
            } else {
                // this is not supported / print a warning
                log.warn("Failed to store the id of the created block storage <{}> for deployment <{}> application <{}> environment <{}>");
            }
        }
    }

    private String getAggregatedVolumeIds(String volumeIds, PaaSInstanceStorageMonitorEvent storageEvent) {
        Set<String> existingVolumes = Sets.newHashSet(volumeIds.split(","));
        if (existingVolumes.contains(storageEvent.getVolumeId())) {
            return null;
        }
        volumeIds = volumeIds + "," + storageEvent.getVolumeId();
        return volumeIds;
    }

    private void updateNodeTemplate(Topology topology, NodeTemplate nodeTemplate, PaaSInstanceStorageMonitorEvent storageEvent, String volumeIds) {
        nodeTemplate.getProperties().put(NormativeBlockStorageConstants.VOLUME_ID, new ScalarPropertyValue(volumeIds));
        log.debug("Updated NodeTemplate <{}.{}> to add VolumeId <{}>. New value is <{}>", topology.getId(), nodeTemplate.getName(), storageEvent.getVolumeId(),
                volumeIds);
        alienDAO.save(topology);
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSInstanceStorageMonitorEvent;
    }
}
