package alien4cloud.paas;

import java.util.Date;
import java.util.Map;

import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.tosca.model.PropertyDefinition;

/**
 * Interface of a Platform as a Service provider.
 */
public interface IPaaSProvider {

    /**
     * Deploy a topology
     * 
     * @param deploymentContext the context of the deployment
     */
    void deploy(PaaSTopologyDeploymentContext deploymentContext);

    /**
     * Undeploy a given topology.
     *
     * @param deploymentContext the context of the un-deployment
     */
    void undeploy(PaaSDeploymentContext deploymentContext);

    /**
     * Scale up/down a node
     *
     * @param deploymentContext the deployment context
     * @param nodeTemplateId id of the compute node to scale up
     * @param instances the number of instances to be added (if positive) or removed (if negative)
     */
    void scale(PaaSDeploymentContext deploymentContext, String nodeTemplateId, int instances);

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
     * Get the deployment property definition
     *
     * @return A map containing property definitions
     */
    Map<String, PropertyDefinition> getDeploymentPropertyMap();
}
