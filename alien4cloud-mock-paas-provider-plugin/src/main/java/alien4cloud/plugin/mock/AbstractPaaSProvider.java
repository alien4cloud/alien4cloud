package alien4cloud.plugin.mock;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.client.RestClientException;

import alien4cloud.model.deployment.DeploymentTopology;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.ScalingPolicy;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.IllegalDeploymentStateException;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.exception.PaaSAlreadyDeployedException;
import alien4cloud.paas.exception.PaaSNotYetDeployedException;
import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Maps;

@Slf4j
public abstract class AbstractPaaSProvider implements IOrchestratorPlugin<ProviderConfig> {
    private ReentrantReadWriteLock providerLock = new ReentrantReadWriteLock();

    @Override
    public void deploy(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<?> callback) {
        String deploymentId = deploymentContext.getDeploymentPaaSId();
        DeploymentTopology deploymentTopology = deploymentContext.getDeploymentTopology();
        try {
            providerLock.writeLock().lock();

            if (deploymentTopology.getProviderDeploymentProperties() != null) {
                // i.e : use / handle plugin deployment properties
                log.info("Topology deployment [" + deploymentTopology.getId() + "] for application [" + deploymentContext.getDeployment().getSourceName() + "]"
                        + " and [" + deploymentTopology.getProviderDeploymentProperties().size() + "] deployment properties");
                log.info(deploymentTopology.getProviderDeploymentProperties().keySet().toString());
                for (String property : deploymentTopology.getProviderDeploymentProperties().keySet()) {
                    log.info(property);
                    if (deploymentTopology.getProviderDeploymentProperties().get(property) != null) {
                        log.info("[ " + property + " : " + deploymentTopology.getProviderDeploymentProperties().get(property) + "]");
                    }
                }
            }

            DeploymentStatus deploymentStatus = getStatus(deploymentId, false);
            switch (deploymentStatus) {
            case DEPLOYED:
            case DEPLOYMENT_IN_PROGRESS:
            case UNDEPLOYMENT_IN_PROGRESS:
            case WARNING:
            case FAILURE:
                throw new PaaSAlreadyDeployedException("Topology [" + deploymentId + "] is in status [" + deploymentStatus + "] and cannot be deployed");
            case UNKNOWN:
                throw new IllegalDeploymentStateException("Topology [" + deploymentId + "] is in status [" + deploymentStatus + "] and cannot be deployed");
            case UNDEPLOYED:
                doDeploy(deploymentContext);
                break;
            default:
                throw new IllegalDeploymentStateException("Topology [" + deploymentId + "] is in illegal status [" + deploymentStatus
                        + "] and cannot be deployed");
            }
        } finally {
            providerLock.writeLock().unlock();
        }
    }

    @Override
    public void getInstancesInformation(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<Map<String, Map<String, InstanceInformation>>> callback) {
        callback.onSuccess(getInstancesInformation(deploymentContext.getDeploymentPaaSId(), deploymentContext.getDeploymentTopology()));
    }

    public Map<String, Map<String, InstanceInformation>> getInstancesInformation(String deploymentId, Topology topology) {
        Map<String, Map<String, InstanceInformation>> instanceInformations = instanceInformationsFromTopology(topology);

        try {
            // TODO :
            // fillInstanceStates(deploymentId, instanceInformations, restEventEndpoint);

            // fillRuntimeInformations(deploymentId, instanceInformations);

            // parseAttributes(instanceInformations, statusByDeployments.get(deploymentId));
            log.debug("------------------------------", instanceInformations);
            return instanceInformations;
        } catch (RestClientException e) {
            log.warn("Error getting " + deploymentId + " deployment informations. \n\t Cause: " + e.getMessage());
            return Maps.newHashMap();
        } catch (Exception e) {
            throw new PaaSTechnicalException("Error getting " + deploymentId + " deployment informations", e);
        }
    }

    private Map<String, Map<String, InstanceInformation>> instanceInformationsFromTopology(Topology topology) {
        Map<String, Map<String, InstanceInformation>> instanceInformations = Maps.newHashMap();
        // fill instance informations based on the topology
        for (Entry<String, NodeTemplate> nodeTempalteEntry : topology.getNodeTemplates().entrySet()) {
            Map<String, InstanceInformation> nodeInstanceInfos = Maps.newHashMap();
            // get the current number of instances
            int currentPlannedInstances = getPlannedInstancesCount(nodeTempalteEntry.getKey(), topology);
            for (int i = 1; i <= currentPlannedInstances; i++) {
                // Map<String, AbstractPropertyValue> properties = nodeTempalteEntry.getValue().getProperties() == null ? null
                // : Maps.newHashMap(nodeTempalteEntry.getValue().getProperties());
                // Map<String, String> attributes = nodeTempalteEntry.getValue().getAttributes() == null ? null
                // : Maps.newHashMap(nodeTempalteEntry.getValue().getAttributes());
                // Map<String, String> runtimeProperties = Maps.newHashMap();
                // TODO remove thoses infos
                // InstanceInformation instanceInfo = new InstanceInformation(ToscaNodeLifecycleConstants.INITIAL, InstanceStatus.PROCESSING, properties,
                // attributes, null);
                // nodeInstanceInfos.put(String.valueOf(i), instanceInfo);
            }
            instanceInformations.put(nodeTempalteEntry.getKey(), nodeInstanceInfos);
        }
        return instanceInformations;
    }

    private int getPlannedInstancesCount(String nodeTemplateId, Topology topology) {
        Capability scalableCapability = TopologyUtils.getScalableCapability(topology, nodeTemplateId, false);
        if (scalableCapability != null) {
            ScalingPolicy scalingPolicy = TopologyUtils.getScalingPolicy(scalableCapability);
            return scalingPolicy.getInitialInstances();
        }
        return 1;
    }

    @Override
    public void setConfiguration(ProviderConfig configuration) throws PluginConfigurationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void undeploy(PaaSDeploymentContext deploymentContext, IPaaSCallback<?> callback) {
        String deploymentId = deploymentContext.getDeploymentPaaSId();
        try {
            providerLock.writeLock().lock();
            DeploymentStatus deploymentStatus = getStatus(deploymentId, true);
            switch (deploymentStatus) {
            case UNDEPLOYMENT_IN_PROGRESS:
            case UNDEPLOYED:
                throw new PaaSNotYetDeployedException("Application [" + deploymentId + "] is in status [" + deploymentStatus + "] and cannot be undeployed");
            case UNKNOWN:
                throw new IllegalDeploymentStateException("Application [" + deploymentId + "] is in status [" + deploymentStatus + "] and cannot be undeployed");
            case DEPLOYMENT_IN_PROGRESS:
            case FAILURE:
            case DEPLOYED:
            case WARNING:
                doUndeploy(deploymentContext);
                break;
            default:
                throw new IllegalDeploymentStateException("Application [" + deploymentId + "] is in illegal status [" + deploymentStatus
                        + "] and cannot be undeployed");
            }
        } finally {
            providerLock.writeLock().unlock();
        }
    }

    public DeploymentStatus getStatus(String deploymentId, boolean triggerEventIfUndeployed) {
        try {
            providerLock.readLock().lock();
            return doGetStatus(deploymentId, triggerEventIfUndeployed);
        } finally {
            providerLock.readLock().unlock();
        }
    }

    protected DeploymentStatus changeStatus(String applicationId, DeploymentStatus status) {
        try {
            providerLock.writeLock().lock();
            return doChangeStatus(applicationId, status);
        } finally {
            providerLock.writeLock().unlock();
        }
    }

    @Override
    public void executeOperation(PaaSTopologyDeploymentContext deploymentContext, NodeOperationExecRequest request, IPaaSCallback<Map<String, String>> callback)
            throws OperationExecutionException {
        try {
            providerLock.writeLock().lock();
            String doExecuteOperationResult = doExecuteOperation(request);
            String resultException = null;
            if (doExecuteOperationResult.equals("KO")) {
                resultException = "Operation execution message when failing...";
            }
            // Raise operation exception
            if (resultException != null) {
                callback.onFailure(new OperationExecutionException(resultException));
            }
            callback.onSuccess(MapUtil.newHashMap(new String[] { "1" }, new String[] { doExecuteOperationResult }));
        } finally {
            providerLock.writeLock().unlock();
        }
    }

    protected abstract DeploymentStatus doChangeStatus(String deploymentId, DeploymentStatus status);

    protected abstract DeploymentStatus doGetStatus(String deploymentId, boolean triggerEventIfUndeployed);

    protected abstract void doDeploy(PaaSTopologyDeploymentContext deploymentContext);

    protected abstract void doUndeploy(PaaSDeploymentContext deploymentContext);

    protected abstract String doExecuteOperation(NodeOperationExecRequest request);
}
