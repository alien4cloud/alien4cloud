package alien4cloud.paas;

import java.util.Date;
import java.util.Map;

import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.tosca.container.model.template.PropertyValue;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.container.model.type.PropertyDefinition;

/**
 * Interface of a Platform as a Service provider.
 */
public interface IPaaSProvider {
    /**
     * Deploy a topology.
     * 
     * @param applicationName Name of the application that owns the topology.
     * @param deploymentId The unique id of the deployment.
     * @param topology The topology to deploy.
     * @param deploymentProperties Deployment plugin properties
     * 
     */
    void deploy(String applicationName, String deploymentId, Topology topology, Map<String, PropertyValue> deploymentProperties);

    /**
     * Undeploy a given topology.
     * 
     * @param deploymentId The id of the topology to undeploy.
     */
    void undeploy(String deploymentId);

    /**
     * Scale up/down a node
     * 
     * @param deploymentId id of the deployment
     * @param nodeTemplateId id of the compute node to scale up
     * @param instances the number of instances to be added (if positive) or removed (if negative)
     */
    void scale(String deploymentId, String nodeTemplateId, int instances);

    /**
     * Get the status of a given topology.
     * 
     * @param deploymentId id of the deployment.
     * @return the deployment status of the topology.
     */
    DeploymentStatus getStatus(String deploymentId);

    /**
     * Get the status of a list of deployments
     * 
     * @param deploymentIds ids of the deployments
     * @return list of deployment status
     */
    DeploymentStatus[] getStatuses(String[] deploymentIds);

    /**
     * Get the detailed status for each instance of each node template.
     * 
     * @param deploymentId
     *            id of the deployment
     * @param topology
     *            The topology for which to get instance information.
     * @return (map : node template's id => (map : instance's id => instance status))
     */
    Map<String, Map<Integer, InstanceInformation>> getInstancesInformation(String deploymentId, Topology topology);

    /**
     * Get all audit events that occurred since the given date. The events must be ordered by date as we could use this method to iterate through events in case
     * of many events.
     * 
     * @param date
     *            The start date since which we should retrieve events.
     * @param maxEvents
     *            The maximum number of events to return.
     * @return An array of time ordered audit events with a maximum size of maxEvents.
     */
    AbstractMonitorEvent[] getEventsSince(Date date, int maxEvents);

    /**
     * Trigger a custom command on a node
     * 
     * @param deploymentId
     *            id of the deployment.
     * @param request
     *            An object of type {@link NodeOperationExecRequest} describing the operation's execution request
     * 
     * @return (map : instance id => operation result on this instance)
     * @throws OperationExecutionException
     */
    Map<String, String> executeOperation(String deploymentId, NodeOperationExecRequest request) throws OperationExecutionException;

    /**
     * Get the deployment property map
     * 
     * @return A property map
     */
    Map<String, PropertyDefinition> getDeploymentPropertyMap();
}