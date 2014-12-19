package alien4cloud.paas.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.tosca.container.model.topology.Topology;

/**
 * The context of the deployment
 */
@Getter
@Setter
@ESObject
@ToString(callSuper = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSTopologyDeploymentContext extends PaaSDeploymentContext {

    private Topology topology;

    private PaaSTopology paaSTopology;

    private DeploymentSetup deploymentSetup;
}