package alien4cloud.paas;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import alien4cloud.paas.exception.MaintenanceModeException;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;

/**
 * Interface of a Platform as a Service provider.
 */
public interface IPaaSProvider {

    /**
     * This method is called by Alien in order to restore the state of the paaS provider after a restart.
     * The provider must implement this method in order to restore its state
     *
     * @param activeDeployments a map of passDeploymentId -> deploymentId (active deployments).
     * @return set of really active passDeploymentIds (those that are effectively known as not undeployed).
     */
    Set<String> init(Map<String, String> activeDeployments);

    /**
     * Deploy a topology
     *
     * @param deploymentContext the context of the deployment
     */
    void deploy(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<?> callback);

    /**
     * Update a topology deployment.
     *
     * @param deploymentContext the context of the deployment
     */
    void update(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<?> callback);

    /**
     * Undeploy a given topology.
     *
     * @param deploymentContext the context of the un-deployment
     * @param force force undeployment in case of errors
     */
    void undeploy(PaaSDeploymentContext deploymentContext, IPaaSCallback<?> callback,boolean force);

    /**
     * Scale up/down a node
     *
     * @param deploymentContext the deployment context
     * @param nodeTemplateId id of the compute node to scale up
     * @param instances the number of instances to be added (if positive) or removed (if negative)
     */
    void scale(PaaSDeploymentContext deploymentContext, String nodeTemplateId, int instances, IPaaSCallback<?> callback);

    /**
     * Launch a workflow.
     *
     * @param deploymentContext the deployment context
     * @param workflowName the workflow to launch
     * @param inputs the workflow params
     * @param callback the callback should be used to return the corresponding executionId
     */
    void launchWorkflow(PaaSDeploymentContext deploymentContext, String workflowName, Map<String, Object> inputs, IPaaSCallback<String> callback);

    /**
     *  Cancel a task
     */
    void cancelTask(PaaSDeploymentContext deploymentContext, String taskId,IPaaSCallback<String> callback);

    /**
     * Get status of a deployment
     *
     * @param deploymentContext the deployment context
     * @param callback callback when the status will be available
     */
    void getStatus(PaaSDeploymentContext deploymentContext, IPaaSCallback<DeploymentStatus> callback);

    /**
     * Get instance information of a topology from the PaaS
     *
     * @param deploymentContext the deployment context
     * @param callback callback when the information will be available
     */
    void getInstancesInformation(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<Map<String, Map<String, InstanceInformation>>> callback);

    /**
     * Get all audit events that occurred since the given date. The events must be ordered by date as we could use this method to iterate through events in case
     * of many events.
     *
     * @param date The start date since which we should retrieve events.
     * @param maxEvents The maximum number of events to return.
     * @return An array of time ordered audit events with a maximum size of maxEvents.
     */
    void getEventsSince(Date date, int maxEvents, IPaaSCallback<AbstractMonitorEvent[]> eventCallback);

    /**
     * Trigger a custom command on a node
     *
     * @param deploymentContext the deployment context
     * @param request An object of type {@link NodeOperationExecRequest} describing the operation's execution request
     * @param operationResultCallback the callback that will be triggered when the operation's result become available
     * @throws OperationExecutionException
     */
    void executeOperation(PaaSTopologyDeploymentContext deploymentContext, NodeOperationExecRequest request,
            IPaaSCallback<Map<String, String>> operationResultCallback) throws OperationExecutionException;

    /**
     * Switch the maintenance mode for this deployed topology.
     *
     * @throws MaintenanceModeException
     */
    void switchMaintenanceMode(PaaSDeploymentContext deploymentContext, boolean maintenanceModeOn) throws MaintenanceModeException;

    /**
     * Switch the maintenance mode for a given node instance of this deployed topology.
     *
     * @throws MaintenanceModeException
     */
    void switchInstanceMaintenanceMode(PaaSDeploymentContext deploymentContext, String nodeId, String instanceId, boolean maintenanceModeOn)
            throws MaintenanceModeException;

}
