package alien4cloud.rest.deployment;

import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.deployment.model.DeploymentConfiguration;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSInstancePersistentResourceMonitorEvent;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import com.google.common.collect.Sets;

@Slf4j
@Component
public class BlockStorageEventHandler extends DeploymentEventHandler {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TopologyServiceCore topologyServiceCore;
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
    private DeploymentTopologyService deploymentTopologyService;

    @Override
    public void eventHappened(AbstractMonitorEvent event) {
        processBlockStorageEvent((PaaSInstancePersistentResourceMonitorEvent) event);
    }

    private void processBlockStorageEvent(PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent) {
        if (StringUtils.isBlank(persistentResourceEvent.getPropertyValue())) {
            return;
        }

        Topology runtimeTopo = alienMonitorDao.findById(Topology.class, persistentResourceEvent.getDeploymentId());
        String volumeIdss = getAggregatedVolumeIds(runtimeTopo, persistentResourceEvent);

        if (StringUtils.isBlank(volumeIdss)) {
            return;
        }

        updateRuntimeTopology(runtimeTopo, persistentResourceEvent, volumeIdss);
        updateApplicationTopology(persistentResourceEvent, volumeIdss);

    }

    private void updateApplicationTopology(PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent, final String volumeIds) {
        // FIXME Update the deployment topology only.
        Deployment deployment = deploymentService.get(persistentResourceEvent.getDeploymentId());
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deployment.getEnvironmentId());
        ApplicationVersion applicationVersion = applicationVersionService.getOrFail(applicationEnvironment.getCurrentVersionId());
        Topology topology = topoServiceCore.getOrFail(applicationVersion.getTopologyId());

        // The deployment topology may have changed and the node removed, in such situations there is nothing to update as the block won't be reused.
        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = topoServiceCore.getNodeTemplate(topology, persistentResourceEvent.getNodeTemplateId());
        } catch (NotFoundException e) {
            log.warn("Fail to update persistent resource property {} for node {}", persistentResourceEvent.getPropertyName(),
                    persistentResourceEvent.getNodeTemplateId(), e);
            return;
        }

        AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(persistentResourceEvent.getPropertyName());
        if (abstractPropertyValue == null || abstractPropertyValue instanceof ScalarPropertyValue) { // the value is set in the topology
            log.info("Updating application topology: Persistent resource node template <{}.{}> to add a value", topology.getId(),
                    persistentResourceEvent.getNodeTemplateId());
            log.debug("Value to add: <{}>. New value is <{}>", persistentResourceEvent.getPropertyValue(), volumeIds);
            nodeTemplate.getProperties().put(persistentResourceEvent.getPropertyName(), new ScalarPropertyValue(volumeIds));
            topologyServiceCore.save(topology);
        } else {
            FunctionPropertyValue function = (FunctionPropertyValue) abstractPropertyValue;
            if (function.getFunction().equals(ToscaFunctionConstants.GET_INPUT)) {
                // the value is set in the input (deployment setup)
                DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.getDeploymentConfiguration(applicationEnvironment.getId());
                DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
                log.info("Updating deploymentsetup <{}> input properties <{}> to add a new VolumeId", deploymentTopology.getId(), function.getTemplateName());
                log.debug("VolumeId to add: <{}>. New value is <{}>", persistentResourceEvent.getPropertyValue(), volumeIds);
                deploymentTopology.getInputProperties().put(function.getTemplateName(), volumeIds);
                alienDAO.save(deploymentTopology);
            } else {
                // this is not supported / print a warning
                log.warn("Failed to store the id of the created block storage <{}> for deployment <{}> application <{}> environment <{}>");
            }
        }
    }

    private void updateRuntimeTopology(Topology runtimeTopo, PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent, String volumeIds) {
        NodeTemplate nodeTemplate = topoServiceCore.getNodeTemplate(runtimeTopo, persistentResourceEvent.getNodeTemplateId());
        log.info("Updating Runtime topology: Storage NodeTemplate <{}.{}> to add a new volumeId", runtimeTopo.getId(),
                persistentResourceEvent.getNodeTemplateId());
        nodeTemplate.getProperties().put(persistentResourceEvent.getPropertyName(), new ScalarPropertyValue(volumeIds));
        log.debug("VolumeId to add: <{}>. New value is <{}>", persistentResourceEvent.getPropertyValue(), volumeIds);
        alienMonitorDao.save(runtimeTopo);
    }

    private String getAggregatedVolumeIds(Topology topology, PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent) {
        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = topoServiceCore.getNodeTemplate(topology, persistentResourceEvent.getNodeTemplateId());
        } catch (NotFoundException e) {
            log.warn("Fail to update volumeIds for node " + persistentResourceEvent.getNodeTemplateId(), e);
            return null;
        }

        AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(persistentResourceEvent.getPropertyName());
        if (abstractPropertyValue instanceof ScalarPropertyValue) { // the value is set in the topology
            String volumeIds = ((ScalarPropertyValue) abstractPropertyValue).getValue();
            return getAggregatedVolumeIds(volumeIds, persistentResourceEvent);
        }
        return persistentResourceEvent.getPropertyValue();
    }

    private String getAggregatedVolumeIds(String volumeIds, PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent) {
        if (volumeIds == null) {
            volumeIds = "";
        }
        Set<String> existingVolumes = Sets.newHashSet(volumeIds.split(","));
        if (existingVolumes.contains(persistentResourceEvent.getPropertyValue())) {
            return null;
        }
        if (StringUtils.isBlank(volumeIds)) {
            volumeIds = persistentResourceEvent.getPropertyValue();
        } else {
            volumeIds = volumeIds + "," + persistentResourceEvent.getPropertyValue();
        }
        return volumeIds;
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSInstancePersistentResourceMonitorEvent;
    }
}
