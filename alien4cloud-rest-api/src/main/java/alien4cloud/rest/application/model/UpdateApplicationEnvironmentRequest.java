package alien4cloud.rest.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.application.EnvironmentType;

/**
 * DTO to update a new application environment
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class UpdateApplicationEnvironmentRequest {
    private EnvironmentType environmentType;
    private String name;
    private String description;
    private String currentVersionId;
}
