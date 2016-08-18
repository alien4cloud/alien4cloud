package alien4cloud.rest.deployment;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.model.orchestrators.locations.Location;

/**
 * Deployment DTO contains the deployment and some informations of the application related.
 *
 * @author igor ngouagna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class DeploymentDTO<T extends IDeploymentSource> {
    private Deployment deployment;
    private T source;
    /* summaries of locations related to the deployments */
    private List<Location> locations;
}
