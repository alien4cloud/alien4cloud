package alien4cloud.rest.application.model;

import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class LaunchWorkflowRequest extends SecretProviderConfigurationAndCredentials {

    private Map<String,Object> inputs;
}
