package alien4cloud.rest.cloud;

import java.util.Map;

import alien4cloud.model.cloud.MatchedCloudImageDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.MatchedCloudImage;
import alien4cloud.model.cloud.MatchedCloudImageFlavor;
import alien4cloud.model.cloud.MatchedNetworkTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudDTO {

    private Cloud cloud;

    private Map<String, MatchedCloudImageDTO> images;

    private Map<String, MatchedCloudImageFlavor> flavors;

    private Map<String, MatchedNetworkTemplate> networks;

    private String[] paaSImageIds;

    private String[] paaSFlavorIds;

    private String[] paaSNetworkTemplateIds;
}
