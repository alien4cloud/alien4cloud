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
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
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
        processBlockStorageEvent((PaaSInstanceStorageMonitorEvent) event);
    }

    private void processBlockStorageEvent(PaaSInstanceStorageMonitorEvent storageEvent) {
        if (StringUtils.isBlank(storageEvent.getVolumeId())) {
            return;
        }

        Topology runtimeTopo = alienMonitorDao.findById(Topology.class, storageEvent.getDeploymentId());
        String volumeIdss = getAggregatedVolumeIds(runtimeTopo, storageEvent);

        if (StringUtils.isBlank(volumeIdss)) {
            return;
        }

        updateRuntimeTopology(runtimeTopo, storageEvent, volumeIdss);
        updateApplicationTopology(storageEvent, volumeIdss);

    }

    private void updateApplicationTopology(PaaSInstanceStorageMonitorEvent storageEvent, final String volumeIds) {
        if (storageEvent.isDeletable()) {
            log.info("Delete blockstorage is activated. Skipping application topology volumeId update...");
            return;
        }

        Deployment deployment = deploymentService.getDeployment(storageEvent.getDeploymentId());
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deployment.getDeploymentSetup().getEnvironmentId());
        ApplicationVersion applicationVersion = applicationVersionService.getOrFail(applicationEnvironment.getCurrentVersionId());
        Topology topology = topoServiceCore.getOrFail(applicationVersion.getTopologyId());

        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = topoServiceCore.getNodeTemplate(topology, storageEvent.getNodeTemplateId());
        } catch (NotFoundException e) {
            log.warn("Fail to update volumeIds for node " + storageEvent.getNodeTemplateId(), e);
            return;
        }

        AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(NormativeBlockStorageConstants.VOLUME_ID);
        if (abstractPropertyValue == null || abstractPropertyValue instanceof ScalarPropertyValue) { // the value is set in the topology
            log.info("Updating application topology: Storage NodeTemplate <{}.{}> to add a new volumeId", topology.getId(), storageEvent.getNodeTemplateId());
            log.debug("VolumeId to add: <{}>. New value is <{}>", storageEvent.getVolumeId(), volumeIds);
            nodeTemplate.getProperties().put(NormativeBlockStorageConstants.VOLUME_ID, new ScalarPropertyValue(volumeIds));
            alienDAO.save(topology);
        } else {
            FunctionPropertyValue function = (FunctionPropertyValue) abstractPropertyValue;
            if (function.getFunction().equals(ToscaFunctionConstants.GET_INPUT)) {
                // the value is set in the input (deployment setup)
                DeploymentSetup deploymentSetup = deploymentSetupService.get(applicationVersion, applicationEnvironment);
                log.info("Updating deploymentsetup <{}> input properties <{}> to add a new VolumeId", deploymentSetup.getId(), function.getTemplateName());
                log.debug("VolumeId to add: <{}>. New value is <{}>", storageEvent.getVolumeId(), volumeIds);
                deploymentSetup.getInputProperties().put(function.getTemplateName(), volumeIds);
                alienDAO.save(deploymentSetup);
            } else {
                // this is not supported / print a warning
                log.warn("Failed to store the id of the created block storage <{}> for deployment <{}> application <{}> environment <{}>");
            }
        }
    }

    private void updateRuntimeTopology(Topology runtimeTopo, PaaSInstanceStorageMonitorEvent storageEvent, String volumeIds) {
        NodeTemplate nodeTemplate = topoServiceCore.getNodeTemplate(runtimeTopo, storageEvent.getNodeTemplateId());
        log.info("Updating Runtime topology: Storage NodeTemplate <{}.{}> to add a new volumeId", runtimeTopo.getId(), storageEvent.getNodeTemplateId());
        nodeTemplate.getProperties().put(NormativeBlockStorageConstants.VOLUME_ID, new ScalarPropertyValue(volumeIds));
        log.debug("VolumeId to add: <{}>. New value is <{}>", storageEvent.getVolumeId(), volumeIds);
        alienMonitorDao.save(runtimeTopo);
    }

    private String getAggregatedVolumeIds(Topology topology, PaaSInstanceStorageMonitorEvent storageEvent) {
        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = topoServiceCore.getNodeTemplate(topology, storageEvent.getNodeTemplateId());
        } catch (NotFoundException e) {
            log.warn("Fail to update volumeIds for node " + storageEvent.getNodeTemplateId(), e);
            return null;
        }

        AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(NormativeBlockStorageConstants.VOLUME_ID);
        if (abstractPropertyValue instanceof ScalarPropertyValue) { // the value is set in the topology
            String volumeIds = ((ScalarPropertyValue) abstractPropertyValue).getValue();
            return getAggregatedVolumeIds(volumeIds, storageEvent);
        }
        return storageEvent.getVolumeId();
    }

    private String getAggregatedVolumeIds(String volumeIds, PaaSInstanceStorageMonitorEvent storageEvent) {
        if (volumeIds == null) {
            volumeIds = "";
        }
        Set<String> existingVolumes = Sets.newHashSet(volumeIds.split(","));
        if (existingVolumes.contains(storageEvent.getVolumeId())) {
            return null;
        }
        if (StringUtils.isBlank(volumeIds)) {
            volumeIds = storageEvent.getVolumeId();
        } else {
            volumeIds = volumeIds + "," + storageEvent.getVolumeId();
        }
        return volumeIds;
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSInstanceStorageMonitorEvent;
    }
}
