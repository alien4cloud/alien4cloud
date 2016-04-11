package alien4cloud.rest.deployment;

import alien4cloud.deployment.model.DeploymentConfiguration;
import alien4cloud.plugin.aop.Overridable;

public interface IDeploymentTopologyHelper {

    @Overridable
    DeploymentTopologyDTO buildDeploymentTopologyDTO(DeploymentConfiguration deploymentConfiguration);

}