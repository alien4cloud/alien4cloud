package alien4cloud.rest.application;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.application.EnvironmentType;

/**
 * DTO to create a new application environment
 * 
 * @author mourouvi
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class ApplicationEnvironmentRequest {
    @NotNull
    private String applicationId;
    private EnvironmentType environmentType;
    private String cloudId;
    private String name;
    private String description;
}
