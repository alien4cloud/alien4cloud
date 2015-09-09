package alien4cloud.deployment;

import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.topology.TopologyValidationResult;

/**
 * Perform validation of a topology before deployment.
 */
public class DeploymentTopologyValidationService {

    /**
     * Perform validation of a deployment topology.
     * 
     * @param deploymentTopology The topology to check.
     * @return A TopologyValidationResult with a list of errors and/or warnings.
     */
    public TopologyValidationResult validateTopology(DeploymentTopology deploymentTopology) {
        // TODO Perform validation of the topology inputs

        // TODO Perform validation of policies
        // If a policy is not matched on the location this is a warning as we allow deployment but some features may be missing
        // If a policy requires a configuration or cannot be applied du to any reason the policy implementation itself can trigger some errors (see Orchestrator
        // plugins)
        TopologyValidationResult validationResult = new TopologyValidationResult();
        validationResult.setValid(true);
        return validationResult;
    }
}
