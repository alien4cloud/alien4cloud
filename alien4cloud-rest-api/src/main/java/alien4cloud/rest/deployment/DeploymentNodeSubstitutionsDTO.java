package alien4cloud.rest.deployment;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceTypes;

@Getter
@Setter
public class DeploymentNodeSubstitutionsDTO {

    private Map<String, List<LocationResourceTemplate>> availableSubstitutions;

    private LocationResourceTypes substitutionTypes;
}
