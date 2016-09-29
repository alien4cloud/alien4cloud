package alien4cloud.rest.application.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class UpdateDeploymentTopologyRequest {

    private Map<String, String> providerDeploymentProperties;

    private Map<String, Object> inputProperties;
}
