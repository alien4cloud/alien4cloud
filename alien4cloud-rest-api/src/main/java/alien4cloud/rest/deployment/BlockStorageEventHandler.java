package alien4cloud.rest.deployment;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSInstancePersistentResourceMonitorEvent;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import lombok.extern.slf4j.Slf4j;

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
    private DeploymentTopologyService deploymentTopologyService;

    @Override
    public void eventHappened(AbstractMonitorEvent event) {
        processBlockStorageEvent((PaaSInstancePersistentResourceMonitorEvent) event);
    }

    private void processBlockStorageEvent(PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent) {
        if (persistentResourceEvent.getPropertyValue() == null
                || (persistentResourceEvent.getPropertyValue() instanceof String && StringUtils.isBlank((String) persistentResourceEvent.getPropertyValue()))) {
            return;
        }

        DeploymentTopology runtimeTopo = alienMonitorDao.findById(DeploymentTopology.class, persistentResourceEvent.getDeploymentId());

        Object propertyValue = persistentResourceEvent.getPropertyValue();
        if (propertyValue instanceof String) {
            propertyValue = getAggregatedVolumeIds(runtimeTopo, persistentResourceEvent.getNodeTemplateId(), persistentResourceEvent.getPropertyName(),
                    (String) propertyValue);

            if (StringUtils.isBlank((String) propertyValue)) {
                return;
            }
        }

        updateRuntimeTopology(runtimeTopo, persistentResourceEvent, propertyValue);
        updateApplicationTopology(persistentResourceEvent, propertyValue);
    }

    private void updateApplicationTopology(PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent, final Object propertyValue) {
        Deployment deployment = deploymentService.get(persistentResourceEvent.getDeploymentId());

        String deploymentTopologyId = DeploymentTopology.generateId(deployment.getVersionId(), deployment.getEnvironmentId());
        DeploymentTopology deploymentTopology = deploymentTopologyService.getOrFail(deploymentTopologyId);

        // The deployment topology may have changed and the node removed, in such situations there is nothing to update as the block won't be reused.
        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = topoServiceCore.getNodeTemplate(deploymentTopology, persistentResourceEvent.getNodeTemplateId());
        } catch (NotFoundException e) {
            log.warn("Fail to update persistent resource property {} for node {}", persistentResourceEvent.getPropertyName(),
                    persistentResourceEvent.getNodeTemplateId(), e);
            return;
        }

        AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(persistentResourceEvent.getPropertyName());
        if (abstractPropertyValue != null && abstractPropertyValue instanceof FunctionPropertyValue) { // the value is set in the topology
            FunctionPropertyValue function = (FunctionPropertyValue) abstractPropertyValue;
            if (function.getFunction().equals(ToscaFunctionConstants.GET_INPUT) && propertyValue instanceof String) {
                // the value is set in the input (deployment setup)
                log.info("Updating deploymentsetup <{}> input properties <{}> to add a new VolumeId", deploymentTopology.getId(), function.getTemplateName());
                log.debug("VolumeId to add: <{}>. New value is <{}>", persistentResourceEvent.getPropertyValue(), propertyValue);
                deploymentTopology.getInputProperties().put(function.getTemplateName(), new ScalarPropertyValue((String) propertyValue));
            } else {
                // this is not supported / print a warning
                log.warn("Failed to store the id of the created block storage <{}> for deployment <{}> application <{}> environment <{}>");
                return;
            }
        } else {
            log.info("Updating deployment topology: Persistent resource property <{}> for node template <{}.{}> to add a value",
                    persistentResourceEvent.getPropertyName(), deploymentTopology.getId(), persistentResourceEvent.getNodeTemplateId());
            log.debug("Value to add: <{}>. New value is <{}>", persistentResourceEvent.getPropertyValue(), propertyValue);
            nodeTemplate.getProperties().put(persistentResourceEvent.getPropertyName(), getPropertyValue(propertyValue));
        }
        deploymentTopologyService.updateDeploymentTopology(deploymentTopology);
    }

    private void updateRuntimeTopology(DeploymentTopology runtimeTopo, PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent,
            Object propertyValue) {
        NodeTemplate nodeTemplate = topoServiceCore.getNodeTemplate(runtimeTopo, persistentResourceEvent.getNodeTemplateId());
        log.info("Updating Runtime topology: Storage NodeTemplate <{}.{}> to add a new volumeId", runtimeTopo.getId(),
                persistentResourceEvent.getNodeTemplateId());
        nodeTemplate.getProperties().put(persistentResourceEvent.getPropertyName(), getPropertyValue(propertyValue));
        log.debug("VolumeId to add: <{}>. New value is <{}>", persistentResourceEvent.getPropertyValue(), propertyValue);
        alienMonitorDao.save(runtimeTopo);
    }

    private PropertyValue getPropertyValue(Object propertyValue) {
        if (propertyValue instanceof String) {
            return new ScalarPropertyValue((String) propertyValue);
        } else if (propertyValue instanceof Map) {
            return new ComplexPropertyValue((Map<String, Object>) propertyValue);
        } else if (propertyValue instanceof List) {
            return new ListPropertyValue((List<Object>) propertyValue);
        }
        log.error("Property value should be a string, a map or a list.", propertyValue);
        return null;
    }

    private String getAggregatedVolumeIds(DeploymentTopology topology, String nodeTemplateId, String propertyName, String propertyValue) {
        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = topoServiceCore.getNodeTemplate(topology, nodeTemplateId);
        } catch (NotFoundException e) {
            log.warn("Fail to update volumeIds for node " + nodeTemplateId, e);
            return null;
        }

        AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(propertyName);
        if (abstractPropertyValue instanceof ScalarPropertyValue) { // the value is set in the topology
            String volumeIds = ((ScalarPropertyValue) abstractPropertyValue).getValue();
            return getAggregatedValues(volumeIds, propertyValue);
        }
        return propertyValue;
    }

    private String getAggregatedValues(String volumeIds, String propertyValue) {
        if (volumeIds == null) {
            volumeIds = "";
        }
        Set<String> existingVolumes = Sets.newHashSet(volumeIds.split(","));
        if (existingVolumes.contains(propertyValue)) {
            return null;
        }
        if (StringUtils.isBlank(volumeIds)) {
            volumeIds = propertyValue;
        } else {
            volumeIds = volumeIds + "," + propertyValue;
        }
        return volumeIds;
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSInstancePersistentResourceMonitorEvent;
    }
}
