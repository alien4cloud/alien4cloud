package alien4cloud.rest.application.model;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.application.EnvironmentType;

/**
 * DTO to create a new application environment
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class ApplicationEnvironmentRequest {
    private String name;
    private EnvironmentType environmentType;
    private String description;
    private String versionId;
}
