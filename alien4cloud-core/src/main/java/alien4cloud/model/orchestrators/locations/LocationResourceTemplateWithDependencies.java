package alien4cloud.model.orchestrators.locations;

import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.common.LocationResourceDeserializer;
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
@ApiModel("Contains a custom resource template with its location's updated dependencies.")
public class LocationResourceTemplateWithDependencies {
    @ApiModelProperty(value = "A custom configured resource template.")
    @JsonDeserialize(using = LocationResourceDeserializer.class)
    AbstractLocationResourceTemplate resourceTemplate;
    @ApiModelProperty(value = "The location's dependencies, which might have been updated when creating the resource template.")
    Set<CSARDependency> newDependencies;
}
