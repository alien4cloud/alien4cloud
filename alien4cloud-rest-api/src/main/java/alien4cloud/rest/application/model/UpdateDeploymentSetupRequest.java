package alien4cloud.rest.application.model;

import java.util.Collection;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.AvailabilityZone;
import alien4cloud.model.cloud.ComputeTemplate;
import alien4cloud.model.cloud.NetworkTemplate;
import alien4cloud.model.cloud.StorageTemplate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class UpdateDeploymentSetupRequest {

    private Map<String, String> providerDeploymentProperties;

    private Map<String, String> inputProperties;

    private Map<String, ComputeTemplate> cloudResourcesMapping;

    private Map<String, NetworkTemplate> networkMapping;

    private Map<String, StorageTemplate> storageMapping;

    private Map<String, Collection<AvailabilityZone>> availabilityZoneMapping;
}
