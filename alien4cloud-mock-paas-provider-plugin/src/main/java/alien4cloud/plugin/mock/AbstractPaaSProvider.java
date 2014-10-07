package alien4cloud.plugin.mock;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.exception.IllegalDeploymentStateException;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.exception.PaaSAlreadyDeployedException;
import alien4cloud.paas.exception.PaaSNotYetDeployedException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.tosca.container.model.template.PropertyValue;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.utils.MapUtil;

@Slf4j
public abstract class AbstractPaaSProvider implements IPaaSProvider {
    private ReentrantReadWriteLock providerLock = new ReentrantReadWriteLock();

    @Override
    public void deploy(String applicationName, String deploymentId, Topology topology, Map<String, PropertyValue> deploymentProperties) {
        try {
            providerLock.writeLock().lock();

            if (deploymentProperties != null) {
                // i.e : use / handle plugin deployment properties
                log.info("Topology deployment [" + topology.getId() + "] for application [" + applicationName + "]" + " and [" + deploymentProperties.size()
                        + "] deployment properties");
                log.info(deploymentProperties.keySet().toString());
                for (String property : deploymentProperties.keySet()) {
                    log.info(property);
                    if (deploymentProperties.get(property) != null) {
                        log.info("[ " + property + " : " + deploymentProperties.get(property).getValue() + "]");
                    }
                }
            }

            DeploymentStatus deploymentStatus = getStatus(deploymentId);
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
                doDeploy(deploymentId);
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
    public void undeploy(String deploymentId) {
        try {
            providerLock.writeLock().lock();
            DeploymentStatus deploymentStatus = getStatus(deploymentId);
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
                doUndeploy(deploymentId);
                break;
            default:
                throw new IllegalDeploymentStateException("Application [" + deploymentId + "] is in illegal status [" + deploymentStatus
                        + "] and cannot be undeployed");
            }
        } finally {
            providerLock.writeLock().unlock();
        }
    }

    @Override
    public DeploymentStatus[] getStatuses(String[] deploymentIds) {
        try {
            providerLock.readLock().lock();
            DeploymentStatus[] status = new DeploymentStatus[deploymentIds.length];
            for (int i = 0; i < deploymentIds.length; i++) {
                status[i] = getStatus(deploymentIds[i]);
            }
            return status;
        } finally {
            providerLock.readLock().unlock();
        }
    }

    @Override
    public DeploymentStatus getStatus(String deploymentId) {
        try {
            providerLock.readLock().lock();
            return doGetStatus(deploymentId);
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
    public Map<String, String> executeOperation(String deploymentId, NodeOperationExecRequest request) throws OperationExecutionException {
        try {
            providerLock.writeLock().lock();
            String doExecuteOperationResult = doExecuteOperation(request);
            String resultException = null;
            if (doExecuteOperationResult.equals("KO")) {
                resultException = "Operation execution message when failing...";
            }
            // Raise operation exception
            if (resultException != null) {
                throw new OperationExecutionException(resultException);
            }
            return MapUtil.newHashMap(new String[] { "1" }, new String[] { doExecuteOperationResult });
        } finally {
            providerLock.writeLock().unlock();
        }
    }

    protected abstract DeploymentStatus doChangeStatus(String deploymentId, DeploymentStatus status);

    protected abstract DeploymentStatus doGetStatus(String deploymentId);

    protected abstract void doDeploy(String deploymentId);

    protected abstract void doUndeploy(String deploymentId);

    protected abstract String doExecuteOperation(NodeOperationExecRequest request);
}
