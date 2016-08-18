package alien4cloud.rest.deployment;

import alien4cloud.model.deployment.IDeploymentSource;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(suppressConstructorProperties = true)
public class DeploymentSourceDTO implements IDeploymentSource {
    private String id;
    private String name;
}
