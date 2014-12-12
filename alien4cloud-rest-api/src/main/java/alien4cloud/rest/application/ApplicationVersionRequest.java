package alien4cloud.rest.application;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * DTO to update a new application version
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class ApplicationVersionRequest {
    @ApiModelProperty(required = true)
    private String version;
    @ApiModelProperty(required = true)
    private String applicationId;
    private String description;
    private boolean released;
}
