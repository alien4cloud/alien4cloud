package alien4cloud.rest.internal.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@ApiModel("Request to validate that a property value is matching a given definition.")
public class PropertyValidationRequest {
    @NotEmpty
    @ApiModelProperty(value = "Value to set for the property.", required = true)
    private String value;
    @NotEmpty
    @ApiModelProperty(value = "Id of the property to set.", required = true)
    private String definitionId;
    @NotEmpty
    @ApiModelProperty(value = "The actual property definition to validate the property against.", required = true)
    private PropertyDefinition propertyDefinition;
}
