package alien4cloud.deployment.model;

import java.util.Map;
import java.util.Set;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceTypes;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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

    @ApiModelProperty(value = "Map of policy id to list of available policy location resource templates' id.")
    private Map<String, Set<String>> availablePoliciesSubstitutions;

    @ApiModelProperty(value = "Map of policy location resource id to policies location resource template.")
    private Map<String, PolicyLocationResourceTemplate> substitutionsPoliciesTemplates;

    @ApiModelProperty(value = "Location resources types contain types for the templates.")
    private LocationResourceTypes substitutionTypes;

    @ApiModelProperty(value = "Map of abstract policies or nodes entities related to each others in such way they have to be in the same location.")
    private Map<String, Set<String>> relatedAbstractEntities;
}