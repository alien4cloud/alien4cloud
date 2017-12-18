package alien4cloud.model.secret;

import lombok.Getter;
import lombok.Setter;

/**
 * The response of secret provider plugin for a authentication request
 */
@Getter
@Setter
public class SecretAuthResponse {
    private Object credentials;
    private Object configuration;
}
