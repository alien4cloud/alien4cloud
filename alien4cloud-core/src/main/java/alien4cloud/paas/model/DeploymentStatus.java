package alien4cloud.paas.model;

/**
 * Status of an application in the orchestrator.
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
     * A deployment has just been triggered, it's in its init stage.
     */
    INIT_DEPLOYMENT,
    /**
     * A deployment has been triggered, it's in progress
     */
    DEPLOYMENT_IN_PROGRESS,
    /**
     * A deployment update has been triggered, it's in progress
     */
    UPDATE_IN_PROGRESS,
    /**
     * A deployment has been updated
     */
    UPDATED,
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
     * An update has failed.
     */
    UPDATE_FAILURE,
    /**
     * Undeployment has failed
     */
    UNDEPLOYMENT_FAILURE,
    /**
     * Paas Provider not reachable, error ...
     */
    UNKNOWN
}
