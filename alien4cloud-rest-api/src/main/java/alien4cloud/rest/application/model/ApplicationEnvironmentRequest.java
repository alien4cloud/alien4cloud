package alien4cloud.rest.application.model;

import alien4cloud.model.application.EnvironmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO to create a new application environment
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEnvironmentRequest {
    private String name;
    private EnvironmentType environmentType;
    private String description;
    private String versionId;
    private String inputCandidate;
}