package alien4cloud.deployment;

import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;

/**
 * A configuration action to execute prior to the execution of the deployment flow.
 */
public interface IDeploymentConfigAction {
    void execute(Application application, ApplicationEnvironment environment, ApplicationTopologyVersion topologyVersion, Topology topology);
}
