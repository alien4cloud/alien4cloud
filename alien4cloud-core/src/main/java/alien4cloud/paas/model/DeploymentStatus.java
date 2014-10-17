package alien4cloud.paas.model;

/**
 * Status of an application on the PaaS
 */
public enum DeploymentStatus {
    /**
     * Application is deployed
     */
    DEPLOYED,
    /**
     * Application is not deployed
     */
    UNDEPLOYED,
    /**
     * A deployment has been triggered, it's in progress
     */
    DEPLOYMENT_IN_PROGRESS,
    /**
     * An undeployment has been triggered, it's in progress
     */
    UNDEPLOYMENT_IN_PROGRESS,
    /**
     * Deployed but with warning
     */
    WARNING,
    /**
     * Application is in abnormal state
     */
    FAILURE,
    /**
     * Paas Provider not reachable, error ...
     */
    UNKNOWN
}
