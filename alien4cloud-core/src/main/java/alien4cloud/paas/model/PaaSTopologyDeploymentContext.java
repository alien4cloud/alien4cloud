package alien4cloud.paas.model;

import java.util.List;
import java.util.Map;

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

    private List<PaaSNodeTemplate> computes;

    private Map<String, PaaSNodeTemplate> nodes;

    private DeploymentSetup deploymentSetup;
}