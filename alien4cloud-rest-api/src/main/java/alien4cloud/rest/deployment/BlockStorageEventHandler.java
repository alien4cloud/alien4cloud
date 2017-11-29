package alien4cloud.rest.deployment;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.services.DeploymentConfigurationDao;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSInstancePersistentResourceMonitorEvent;
import alien4cloud.topology.TopologyServiceCore;
import org.alien4cloud.tosca.utils.TopologyUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BlockStorageEventHandler extends DeploymentEventHandler {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Resource
    private DeploymentConfigurationDao deploymentConfigurationDao;
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
        if (persistentResourceEvent.getPersistentProperties() == null || (persistentResourceEvent.getPersistentProperties().isEmpty())) {
            return;
        }

        DeploymentTopology runtimeTopo = alienMonitorDao.findById(DeploymentTopology.class, persistentResourceEvent.getDeploymentId());

        Map<String, Object> persistentProperties = persistentResourceEvent.getPersistentProperties();
        if (!persistentProperties.isEmpty() && valuesAreAllString(persistentProperties)) {
            persistentProperties = getAggregatedVolumeIds(runtimeTopo, persistentResourceEvent.getNodeTemplateId(), persistentProperties);

            if (persistentProperties.isEmpty()) {
                return;
            }
        }

        updateRuntimeTopology(runtimeTopo, persistentResourceEvent, persistentProperties);
        updateApplicationTopology(persistentResourceEvent, persistentProperties);
    }

    private boolean valuesAreAllString(Map<String, Object> persistentProperties) {
        for (Object value : persistentProperties.values()) {
            if (!(value instanceof String)) {
                return false;
            }
        }
        return true;
    }

    private void updateApplicationTopology(PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent, final Map<String, Object> persistentProperties) {
        Deployment deployment = deploymentService.get(persistentResourceEvent.getDeploymentId());

        String topologyId = deployment.getSourceId() + ":" + deployment.getVersionId();
        Topology topology = topoServiceCore.getOrFail(topologyId);

        // The deployment topology may have changed and the node removed, in such situations there is nothing to update as the block won't be reused.
        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = TopologyUtils.getNodeTemplate(topology, persistentResourceEvent.getNodeTemplateId());
        } catch (NotFoundException e) {
            log.warn("Fail to update persistent resource for node {}", persistentResourceEvent.getNodeTemplateId(), e);
            return;
        }

        for (String propertyName : persistentProperties.keySet()) {
            Object propertyValue = persistentProperties.get(propertyName);
            AbstractPropertyValue abstractPropertyValue = nodeTemplate.getProperties().get(propertyName);
            if (abstractPropertyValue != null && abstractPropertyValue instanceof FunctionPropertyValue) { // the value is set in the topology
                FunctionPropertyValue function = (FunctionPropertyValue) abstractPropertyValue;
                if (function.getFunction().equals(ToscaFunctionConstants.GET_INPUT) && propertyValue instanceof String) {
                    DeploymentInputs deploymentInputs = deploymentConfigurationDao.findById(DeploymentInputs.class,
                            AbstractDeploymentConfig.generateId(deployment.getVersionId(), deployment.getEnvironmentId()));
                    // the value is set in the input (deployment setup)
                    log.info("Updating deploymentsetup [ {} ] input properties [ {} ] to add a new VolumeId", deploymentInputs.getId(), function.getTemplateName());
                    log.debug("Property [ {} ] to update: [ {} ]. New value is [ {} ]", propertyName,
                            persistentResourceEvent.getPersistentProperties().get(propertyName), propertyValue);
                    deploymentInputs.getInputs().put(function.getTemplateName(), new ScalarPropertyValue((String) propertyValue));
                    deploymentConfigurationDao.save(deploymentInputs);
                } else {
                    // this is not supported / print a warning
                    log.warn("Failed to store the id of the created block storage [ {} ] for deployment [ {} ] application [ {} ] environment [ {} ]");
                    return;
                }
            } else {
                DeploymentMatchingConfiguration matchingConfiguration = deploymentConfigurationDao.findById(DeploymentMatchingConfiguration.class,
                        AbstractDeploymentConfig.generateId(deployment.getVersionId(), deployment.getEnvironmentId()));
                log.info("Updating deployment topology: Persistent resource property [ {} ] for node template <{}.{}> to add a value", propertyName,
                        matchingConfiguration.getId(), persistentResourceEvent.getNodeTemplateId());
                log.debug("Value to add: [ {} ]. New value is [ {} ]", persistentResourceEvent.getPersistentProperties().get(propertyName), propertyValue);
                matchingConfiguration.getMatchedNodesConfiguration().get(persistentResourceEvent.getNodeTemplateId()).getProperties().put(propertyName,
                        getPropertyValue(propertyValue));
                deploymentConfigurationDao.save(matchingConfiguration);
            }
        }
    }

    private void updateRuntimeTopology(DeploymentTopology runtimeTopo, PaaSInstancePersistentResourceMonitorEvent persistentResourceEvent,
            Map<String, Object> persistentProperties) {
        NodeTemplate nodeTemplate = TopologyUtils.getNodeTemplate(runtimeTopo, persistentResourceEvent.getNodeTemplateId());
        log.info("Updating Runtime topology: Storage NodeTemplate <{}.{}> to add a new volumeId", runtimeTopo.getId(),
                persistentResourceEvent.getNodeTemplateId());
        for (String key : persistentProperties.keySet()) {
            nodeTemplate.getProperties().put(key, getPropertyValue(persistentProperties.get(key)));
            log.debug("Property [ {} ] to update: [ {} ]. New value is [ {} ]", key, persistentResourceEvent.getPersistentProperties().get(key),
                    persistentProperties.get(key));
        }
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

    private Map<String, Object> getAggregatedVolumeIds(DeploymentTopology topology, String nodeTemplateId, Map<String, Object> persistentProperties) {
        NodeTemplate nodeTemplate;
        try {
            nodeTemplate = TopologyUtils.getNodeTemplate(topology, nodeTemplateId);
        } catch (NotFoundException e) {
            log.warn("Fail to update volumeIds for node " + nodeTemplateId, e);
            return null;
        }

        Map<String, Object> aggregatedProperties = Maps.newHashMap();

        Map<String, Object> concernedNodeProperties = extractConcernedNodeProperties(nodeTemplate.getName(), nodeTemplate.getProperties(),
                persistentProperties.keySet());
        List<Map<String, String>> splittedNodeProperties = splitNodePropertiesValue(concernedNodeProperties);
        if (!persistentPropertiesAlreadyExist(persistentProperties, splittedNodeProperties)) {
            aggregatedProperties = buildAggregatedProperties(persistentProperties, concernedNodeProperties);
        }

        return aggregatedProperties;
    }

    private Map<String, Object> buildAggregatedProperties(Map<String, Object> persistentProperties, Map<String, Object> concernedNodeProperties) {
        Map<String, Object> aggregatedProperties = Maps.newHashMap();
        for (String key : persistentProperties.keySet()) {
            String existingValues = (String) concernedNodeProperties.get(key);
            String newValue = (String) persistentProperties.get(key);
            String aggregatedValue = "";
            if (StringUtils.isBlank(existingValues)) {
                aggregatedValue = newValue;
            } else {
                aggregatedValue = existingValues + "," + newValue;
            }
            aggregatedProperties.put(key, aggregatedValue);
        }
        return aggregatedProperties;
    }

    private boolean persistentPropertiesAlreadyExist(Map<String, Object> persistentProperties, List<Map<String, String>> splittedNodeProperties) {
        int nbProperties = persistentProperties.size();
        for (Map<String, String> existing : splittedNodeProperties) {
            int nbPropertiesEquals = 0;
            for (String key : persistentProperties.keySet()) {
                if (StringUtils.equals((String) persistentProperties.get(key), existing.get(key))) {
                    nbPropertiesEquals++;
                }
            }
            if (nbPropertiesEquals == nbProperties) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, String>> splitNodePropertiesValue(Map<String, Object> concernedNodeProperties) {
        List<Map<String, String>> list = Lists.newArrayList();

        int valueLength = 0;
        if (concernedNodeProperties.values().iterator().hasNext()) {
            String plainTextValue = (String) concernedNodeProperties.values().iterator().next();
            valueLength = plainTextValue.split(",").length;
        }

        for (int i = 0; i < valueLength; i++) {
            Map<String, String> line = Maps.newHashMap();
            for (String key : concernedNodeProperties.keySet()) {
                String plainValue = (String) concernedNodeProperties.get(key);
                line.put(key, getValueFromIndex(i, plainValue));
            }
            list.add(line);
        }

        return list;
    }

    private String getValueFromIndex(int i, String plainValue) {
        String[] splitted = plainValue.split(",");
        if (splitted != null && i < splitted.length) {
            return splitted[i];
        }
        throw new IllegalStateException("Properties do not have the same number of instance values.");
    }

    private Map<String, Object> extractConcernedNodeProperties(String nodeName, Map<String, AbstractPropertyValue> nodeProperties, Set<String> keys) {
        Map<String, Object> extractedMap = Maps.newHashMap();
        for (String key : keys) {
            AbstractPropertyValue abstractPropertyValue = nodeProperties.get(key);
            if (abstractPropertyValue instanceof ScalarPropertyValue) {
                String existingValue = ((ScalarPropertyValue) abstractPropertyValue).getValue();
                extractedMap.put(key, existingValue);
            } else {
                // Can't aggregate values
                log.debug("Fail to aggregate persistent value for node {} as property key {} is not a scalar type", nodeName, key);
                return Maps.newHashMap();
            }
        }
        return extractedMap;
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSInstancePersistentResourceMonitorEvent;
    }
}
