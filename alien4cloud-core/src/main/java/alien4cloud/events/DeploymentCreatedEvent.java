package alien4cloud.events;

import lombok.Getter;

/**
 * An event published when a {@link alien4cloud.model.deployment.Deployment} is created.
 */
@Getter
public class DeploymentCreatedEvent extends AlienEvent {

    private static final long serialVersionUID = 4523559660777770642L;

    private String deploymentId;

    public DeploymentCreatedEvent(Object source, String deploymentId) {
        super(source);
        this.deploymentId = deploymentId;
    }

}
