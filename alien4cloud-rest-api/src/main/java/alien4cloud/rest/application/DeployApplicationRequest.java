package alien4cloud.rest.application;

import java.util.Map;

import alien4cloud.tosca.container.model.template.PropertyValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class DeployApplicationRequest {
    private String applicationId;
    private Map<String, PropertyValue> deploymentProperties;
}
