package alien4cloud.rest.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.application.EnvironmentType;

/**
 * DTO to update a new application environment
 * 
 * @author mourouvi
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class UpdateApplicationEnvironmentRequest {
    private EnvironmentType environmentType;
    private String cloudId;
    private String name;
    private String description;
    private String currentVersionId;
}
