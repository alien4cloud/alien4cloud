package alien4cloud.deployment.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecretProviderCredentials {
    private Object credentials;
    private String pluginName;
}
