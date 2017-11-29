package alien4cloud.rest.orchestrator.model;

import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.rest.secret.model.SecretProviderConfigurationsDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationDTO {

    private Location location;

    private LocationResources resources;

    private SecretProviderConfigurationsDTO secretProviderConfigurations;
}
