package alien4cloud.rest.internal.model;

import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.hibernate.validator.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Request to validate that a property value is matching a given definition.")
public class PropertyValidationRequest {
    @ApiModelProperty(value = "Value to set for the property.", required = true)
    private Object value;
    @NotEmpty
    @ApiModelProperty(value = "Id of the property to set.", required = true)
    private String definitionId;
    @NotEmpty
    @ApiModelProperty(value = "The actual property definition to validate the property against.", required = true)
    private PropertyDefinition propertyDefinition;

    @ApiModelProperty(value = "Dependencies to search for data types.")
    private Set<CSARDependency> dependencies;
}
