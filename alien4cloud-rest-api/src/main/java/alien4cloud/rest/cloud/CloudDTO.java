package alien4cloud.rest.cloud;

import java.util.Map;

import alien4cloud.model.cloud.CloudImageFlavor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudImage;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudDTO {

    private Cloud cloud;

    private Map<String, CloudImage> images;

    private Map<String, CloudImageFlavor> flavors;
}
