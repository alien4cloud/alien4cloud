package alien4cloud.deployment.model;

import java.util.Map;
import java.util.Set;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("Contains the types and templates of resources that can be substituted for a deployment.")
public class DeploymentSubstitutionConfiguration {

    @ApiModelProperty(value = "Map of node id to list of available location resource templates' id.")
    private Map<String, Set<String>> availableSubstitutions;

    @ApiModelProperty(value = "Map of location resource id to location resource template.")
    private Map<String, LocationResourceTemplate> substitutionsTemplates;

    @ApiModelProperty(value = "Location resources types contain types for the templates.")
    private LocationResourceTypes substitutionTypes;
}
