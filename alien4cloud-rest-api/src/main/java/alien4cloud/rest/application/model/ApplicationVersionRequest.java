package alien4cloud.rest.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.annotations.ApiModelProperty;

/**
 * DTO to update a new application version
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class ApplicationVersionRequest {
    @ApiModelProperty(required = true)
    private String version;
    private String description;
    private String topologyId;
}
