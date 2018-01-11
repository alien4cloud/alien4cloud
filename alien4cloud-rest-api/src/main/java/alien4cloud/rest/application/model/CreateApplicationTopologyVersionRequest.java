package alien4cloud.rest.application.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * Create a new topology version for an application.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "Request to set locations policies for a deployment.")
public class CreateApplicationTopologyVersionRequest {
    @ApiModelProperty(value = "Qualifier string that allow having a distinct topology version for every Application Topology Version in an Application Version.", notes = "The id of the topology version is build by concatenation of the Application version and the given qualifier.")
    private String qualifier;
    @ApiModelProperty(value = "Description for this specific variant of the topology for the application version.")
    private String description;
    @ApiModelProperty(value = "Id of the topology template to use to initialize the application topology version that will be created with the new application version.", notes = "This field is mutually exclusive with applicationVersionId and applicationTopologyVersionId and is used only to create a new application version with a single topology version based on the given template.")
    private String topologyTemplateId;
    @ApiModelProperty(value = "Id of the application topology version to use to initialize this application topology versions.", notes = "This field is mutually exclusive with topologyTemplateId. This field must be used in conjunction with applicationVersionId.")
    private String applicationTopologyVersion;
}