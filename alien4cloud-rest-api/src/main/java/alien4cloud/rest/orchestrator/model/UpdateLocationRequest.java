package alien4cloud.rest.orchestrator.model;

import alien4cloud.model.secret.SecretProviderConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLocationRequest {
    private String name;
    private String environmentType;
    private SecretProviderConfiguration secretProviderConfiguration;
}