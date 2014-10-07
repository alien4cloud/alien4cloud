package alien4cloud.rest.deployment;

import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.IDeploymentSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deployment DTO contains the deployment and some informations of the application related.
 * 
 * @author igor ngouagna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class DeploymentDTO<T extends IDeploymentSource> {
    private Deployment deployment;
    private T source;
}
