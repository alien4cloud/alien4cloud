package alien4cloud.rest.application;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO to update a new application version
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class UpdateApplicationVersionRequest {
    private String id;
    private String version;
    private String description;
    private String applicationId;
    private boolean released;
}
