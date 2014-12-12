package alien4cloud.rest.application;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationCloudRequest {
    private String cloudId;
    private String applicationEnvironmentId;
}
