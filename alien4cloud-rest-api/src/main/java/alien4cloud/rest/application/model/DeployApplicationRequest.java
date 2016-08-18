package alien4cloud.rest.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class DeployApplicationRequest {
    private String applicationId;
    private String applicationEnvironmentId;
}
