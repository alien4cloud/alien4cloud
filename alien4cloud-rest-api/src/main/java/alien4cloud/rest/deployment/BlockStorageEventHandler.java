package alien4cloud.rest.deployment;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStorageMonitorEvent;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.normative.AlienCustomTypes;
import alien4cloud.tosca.normative.NormativeBlockStorageConstants;

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

    @Override
    public void eventHappened(AbstractMonitorEvent event) {
        checkAndProcessBlockStorageEvent((PaaSInstanceStorageMonitorEvent) event);
    }

    private void checkAndProcessBlockStorageEvent(PaaSInstanceStorageMonitorEvent storageEvent) {
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

        if (ToscaUtils.isFromType(AlienCustomTypes.DELETABLE_BLOCKSTORAGE_TYPE, topoServiceCore.getRelatedIndexedNodeType(nodeTemplate, topology))) {
            log.info("Blockstorage <{}.{}> is a deletable type. Skipping topology volumeId update...", topology.getId(), nodeTemplate.getName());
            return;
        }

        String volumeIds = nodeTemplate.getProperties().get(NormativeBlockStorageConstants.VOLUME_ID);
        if (StringUtils.isNotBlank(storageEvent.getVolumeId())) {
            if (volumeIds == null) {
                volumeIds = "";
            } else {
                volumeIds += ",";
            }
            volumeIds += storageEvent.getVolumeId();
        }
        nodeTemplate.getProperties().put(NormativeBlockStorageConstants.VOLUME_ID, volumeIds);
        log.info("Updated NodeTemplate <{}.{}> to add VolumeId <{}> in position <{}> . New value is <{}>", topology.getId(), nodeTemplate.getName(),
                storageEvent.getVolumeId(), storageEvent.getInstanceId(), volumeIds);
        alienDAO.save(topology);
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSInstanceStorageMonitorEvent;
    }
}
