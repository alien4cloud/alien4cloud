package alien4cloud.paas;

import java.util.Date;
import java.util.Map;

import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.model.PropertyDefinition;

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
     * @param deploymentSetup Deployment set up
     */
    void deploy(String applicationName, String deploymentId, Topology topology, DeploymentSetup deploymentSetup);

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
     * @param deploymentId id of the deployment.
     * @param request An object of type {@link NodeOperationExecRequest} describing the operation's execution request
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
