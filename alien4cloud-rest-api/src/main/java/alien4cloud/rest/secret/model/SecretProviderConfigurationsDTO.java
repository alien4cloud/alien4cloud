package alien4cloud.rest.secret.model;

import java.util.Map;

import alien4cloud.model.secret.SecretProviderConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SecretProviderConfigurationsDTO {
    Map<String, Map<String, Object>> genericFormByPluginName;
    SecretProviderConfiguration currentConfiguration;
}
