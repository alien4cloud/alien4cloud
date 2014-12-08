package alien4cloud.rest.application;

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
    private String applicationEnvironmentId;
    private String applicationVersionId;
}
