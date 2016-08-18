package alien4cloud.rest.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.paas.model.DeploymentStatus;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
public class EnvironmentStatusDTO {
    private String environmentName;
    private DeploymentStatus environmentStatus;
}
