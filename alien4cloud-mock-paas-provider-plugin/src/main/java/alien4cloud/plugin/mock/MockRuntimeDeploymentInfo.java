package alien4cloud.plugin.mock;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
public class MockRuntimeDeploymentInfo {
    private PaaSTopologyDeploymentContext deploymentContext;
    private DeploymentStatus status;
    /**
     * Represents the status of every instance of node templates currently deployed.
     * 
     * NodeTemplateId -> InstanceId -> InstanceInformation
     */
    private Map<String, Map<String, InstanceInformation>> instanceInformations;

}
