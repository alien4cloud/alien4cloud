package alien4cloud.rest.application;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.ComputeTemplate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class UpdateDeploymentSetupRequest {

    private Map<String, String> providerDeploymentProperties;

    private Map<String, String> inputProperties;

    private Map<String, ComputeTemplate> cloudResourcesMapping;
}
