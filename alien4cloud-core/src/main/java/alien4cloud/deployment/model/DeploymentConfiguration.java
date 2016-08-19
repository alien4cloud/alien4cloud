package alien4cloud.deployment.model;

import alien4cloud.model.deployment.DeploymentTopology;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class DeploymentConfiguration {

    private DeploymentTopology deploymentTopology;

    private DeploymentSubstitutionConfiguration availableSubstitutions;
}
