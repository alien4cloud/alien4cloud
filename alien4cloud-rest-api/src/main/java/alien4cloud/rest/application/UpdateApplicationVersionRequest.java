package alien4cloud.rest.application;

import java.util.Map;

import com.wordnik.swagger.annotations.ApiModelProperty;
import com.wordnik.swagger.annotations.ApiParam;

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
	@ApiModelProperty(required=true)
    private String version;
	@ApiModelProperty(required=true)
    private String applicationId;
    private String description;
    private boolean released;
}
