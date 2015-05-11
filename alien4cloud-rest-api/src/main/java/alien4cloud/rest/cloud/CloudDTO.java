package alien4cloud.rest.cloud;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.MatchedAvailabilityZone;
import alien4cloud.model.cloud.MatchedCloudImage;
import alien4cloud.model.cloud.MatchedCloudImageFlavor;
import alien4cloud.model.cloud.MatchedNetworkTemplate;
import alien4cloud.model.cloud.MatchedStorageTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudDTO {

    private Cloud cloud;

    private Map<String, MatchedCloudImage> images;

    private Map<String, MatchedCloudImageFlavor> flavors;

    private Map<String, MatchedNetworkTemplate> networks;

    private Map<String, MatchedStorageTemplate> storages;

    private Map<String, MatchedAvailabilityZone> zones;

    private String[] paaSImageIds;

    private String[] paaSFlavorIds;

    private String[] paaSNetworkTemplateIds;

    private String[] paaSStorageTemplateIds;
}
