package alien4cloud.rest.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * DTO to create a new application version
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationVersionRequest {
    @NotNull
    @ApiModelProperty(required = true)
    private String version;
    private String description;
    // Create from topologyTemplate
    @ApiModelProperty(value = "Id of the topology template to use to initialize the application topology version that will be created with the new application version.", notes = "This field is mutually exclusive with previousVersionId and is used only to create a new application version with a single topology version based on the given template.")
    private String topologyTemplateId;
    // Create from previous version
    @ApiModelProperty(value = "Id of the application version to use to initialize all application topology versions.", notes = "This field is mutually exclusive with topologyTemplateId and is used only to create a new application version having all topology versions created based on previous versions.")
    private String fromVersionId;
}