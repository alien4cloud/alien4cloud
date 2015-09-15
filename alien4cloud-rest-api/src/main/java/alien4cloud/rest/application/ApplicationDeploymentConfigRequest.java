package alien4cloud.rest.application;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationDeploymentConfigRequest {
    private String environmentId;
    private String locationId;
}
