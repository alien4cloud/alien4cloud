package alien4cloud.paas;

import java.util.Date;
import java.util.Map;

import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.cloud.CloudResourceType;
import alien4cloud.model.topology.Topology;
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
     * Deploy a topology
     * 
     * @param deploymentContext the context of the deployment
     */
    void deploy(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<?> callback);

    /**
     * Undeploy a given topology.
     *
     * @param deploymentContext the context of the un-deployment
     */
    void undeploy(PaaSDeploymentContext deploymentContext, IPaaSCallback<?> callback);

    /**
     * Scale up/down a node
     *
     * @param deploymentContext the deployment context
     * @param nodeTemplateId id of the compute node to scale up
     * @param instances the number of instances to be added (if positive) or removed (if negative)
     */
    void scale(PaaSDeploymentContext deploymentContext, String nodeTemplateId, int instances, IPaaSCallback<?> callback);

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
     * @param topology the topology to retrieve information
     * @param callback callback when the information will be available
     */
    void getInstancesInformation(PaaSDeploymentContext deploymentContext, Topology topology,
            IPaaSCallback<Map<String, Map<String, InstanceInformation>>> callback);

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
    void executeOperation(PaaSDeploymentContext deploymentContext, NodeOperationExecRequest request, IPaaSCallback<Map<String, String>> operationResultCallback)
            throws OperationExecutionException;

    /**
     * Call to determine available ids for the given resource type
     *
     * @param resourceType the type of the resource
     * @return ids for the given resource type
     */
    String[] getAvailableResourceIds(CloudResourceType resourceType);

    /**
     * Call to determine available ids for the given resource type restricted to the given image.
     * Many resources are available only on a certain type of image.
     *
     * @param resourceType the type of the resource
     * @param imageId id for the image
     * @return ids for the given resource type
     */
    String[] getAvailableResourceIds(CloudResourceType resourceType, String imageId);

    /**
     * Call to initialize or notify the paaS provider about matcher configuration change
     *
     * @param config the config to take into account
     */
    void updateMatcherConfig(CloudResourceMatcherConfig config);
}
