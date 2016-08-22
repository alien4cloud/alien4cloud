package alien4cloud.rest.internal.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.PropertyDefinition;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@ApiModel("Request to update or check the value of a property.")
public class PropertyRequest {
    @NotEmpty
    @ApiModelProperty(value = "Value to set for the property.", required = true)
    private String value;
    @NotEmpty
    @ApiModelProperty(value = "Id of the property to set.", required = true)
    private String definitionId;
}