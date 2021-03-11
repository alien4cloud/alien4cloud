package alien4cloud.events;

import lombok.Getter;

/**
 * At startup, potentially after a downtime, each orchestrator plugin try to reconciliate with orchestrator status.
 * This event is published when an active deployment has been detected on orchestrator.
 */
@Getter
public class DeploymentRecoveredEvent extends AlienEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private String deploymentId;

    public DeploymentRecoveredEvent(Object source, String deploymentId) {
        super(source);
        this.deploymentId = deploymentId;
    }

}
