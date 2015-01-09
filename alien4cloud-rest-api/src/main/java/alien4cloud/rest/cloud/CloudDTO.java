package alien4cloud.rest.cloud;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.Network;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudDTO {

    private Cloud cloud;

    private CloudResourceMatcherDTO cloudResourceMatcher;

    private Map<String, CloudImage> images;

    private Map<String, CloudImageFlavor> flavors;

    private Map<String, Network> networks;
}
