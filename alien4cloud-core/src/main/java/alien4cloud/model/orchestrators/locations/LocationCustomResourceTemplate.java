package alien4cloud.model.orchestrators.locations;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.CSARDependency;

import java.util.Set;

/**
 * TODO javadoc + Api model
 */
@Getter
@Setter
@AllArgsConstructor
@ApiModel("Contains a custom resource template with its location's updated dependencies.")
public class LocationCustomResourceTemplate {
    @ApiModelProperty(value = "A custom configured resource template.")
    LocationResourceTemplate resourceTemplate;
    @ApiModelProperty(value = "The location's dependencies, which might have been updated when creating the resource template.")
    Set<CSARDependency> newDependencies;
}
