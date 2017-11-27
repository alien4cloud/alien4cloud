package alien4cloud.deployment;

import java.util.function.Supplier;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;

/**
 * Interface for the deployment topology dto builder that allows to map a deployment flow execution to a deployment topology dto.
 */
public interface IDeploymentTopologyBuilder {
    /**
     * Create a deployment topology DTO after the execution of the deployment flow on the given topology.
     *
     * @param topology The topology on which to execute the deployment flow to build the deployment topology DTO.
     * @param application The application that holds the topology.
     * @param environment The environment related to the deployment configuration.
     * @return A Deployment Topology DTO.
     */
    DeploymentTopologyDTO prepareDeployment(Topology topology, Application application, ApplicationEnvironment environment);

    /**
     * Create a deployment topology DTO after the execution of the deployment flow on the given topology.
     *
     * Prior to the execution of the deployment flow a given action is executed to allow configuration update.
     *
     * @param topology The topology on which to execute the deployment flow to build the deployment topology DTO.
     * @param application The application that holds the topology.
     * @param environment The environment related to the deployment configuration.
     * @param topologyVersion The version associated with the deployment.
     * @param deploymentConfigAction A callback to trigger the execution of an action in the same TOSCA context as the one of the topology and flow execution.
     * @return A Deployment Topology DTO.
     */
    DeploymentTopologyDTO prepareDeployment(Topology topology, Application application, ApplicationEnvironment environment,
            ApplicationTopologyVersion topologyVersion, IDeploymentConfigAction deploymentConfigAction);

    /**
     * Create a deployment topology DTO after the of the context supplier method that is responsible for execution a Deployment Flow and providing back the
     * result of the flow execution.
     *
     * @param topology The topology on which to execute the deployment flow to build the deployment topology DTO. Used for TOSCA context creation
     * @param contextSupplier The context supplier responsible for the providing of the FlowExecutionContext after execution of the deployment flow.
     * @return A Deployment Topology DTO.
     */
    DeploymentTopologyDTO prepareDeployment(Topology topology, Supplier<FlowExecutionContext> contextSupplier);
}