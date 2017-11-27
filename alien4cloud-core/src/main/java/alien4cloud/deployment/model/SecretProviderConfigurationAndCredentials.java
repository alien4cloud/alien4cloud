package alien4cloud.deployment.model;

import alien4cloud.model.secret.SecretProviderConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecretProviderConfigurationAndCredentials {
    private SecretProviderConfiguration secretProviderConfiguration;
    private Object credentials;
}
