package alien4cloud.deployment.model;

import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.DeploymentTopology;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeploymentConfiguration {
    private DeploymentTopology deploymentTopology;

    private DeploymentSubstitutionConfiguration availableSubstitutions;
}
