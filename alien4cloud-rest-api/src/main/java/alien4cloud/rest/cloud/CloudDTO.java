package alien4cloud.rest.cloud;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudDTO {

    private Cloud cloud;

    private CloudResourceMatcherConfig matcherConfig;

    private Map<String, CloudImage> images;

    private Map<String, CloudImageFlavor> flavors;
}
