package alien4cloud.paas.exception;

/**
 * This exception is thrown in case the deployment id generated for an orchestrator is used by another deployment.
 */
public class OrchestratorDeploymentIdConflictException extends PaaSTechnicalException {
    private static final long serialVersionUID = 1L;

    public OrchestratorDeploymentIdConflictException(String message) {
        super(message);
    }
}