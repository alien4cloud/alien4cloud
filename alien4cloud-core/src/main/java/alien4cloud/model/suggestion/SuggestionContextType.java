package alien4cloud.model.suggestion;

public enum SuggestionContextType {

    /** The user is editing a topology. */
    TopologyEdit,

    /**
     * The user is setting deployment inputs.
     */
    DeploymentInput,

    /**
     * The user is setting an orchestrator resource.
     */
    OrchestratorResourceConfiguration,

    /**
     * The user is setting an orchestrator policy.
     */
    OrchestratorPolicyConfiguration,

    /**
     * The user is setting a node matching during a deployment flow.
     */
    DeploymentNodeMatching,

    /**
     * The user is setting a policy matching during a deployment flow.
     */
    DeploymentPolicyMatching,

    /**
     * The user is setting a service (admin).
     */
    ServiceConfiguration
}
