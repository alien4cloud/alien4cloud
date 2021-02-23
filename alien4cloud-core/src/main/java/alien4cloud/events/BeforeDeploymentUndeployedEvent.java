package alien4cloud.events;

import lombok.Getter;

/**
 * An event published when a {@link alien4cloud.model.deployment.Deployment} is undeployed.
 */
@Getter
public class BeforeDeploymentUndeployedEvent extends AlienEvent {

    private static final long serialVersionUID = -8956870370083387625L;

    private String deploymentId;

    public BeforeDeploymentUndeployedEvent(Object source, String deploymentId) {
        super(source);
        this.deploymentId = deploymentId;
    }
}
