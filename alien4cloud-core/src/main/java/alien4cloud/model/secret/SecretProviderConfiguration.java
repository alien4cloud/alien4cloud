package alien4cloud.model.secret;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecretProviderConfiguration {
    private String pluginName;
    private Object configuration;
}
