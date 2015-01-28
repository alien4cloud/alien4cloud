package alien4cloud.cloud;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.NetworkTemplate;
import alien4cloud.model.cloud.StorageTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudResourceTopologyMatchResult {

    /**
     * Image match result contains couple of node template id --> eligible images
     */
    private Map<String, List<CloudImage>> imageMatchResult;

    /**
     * Flavor match result contains couple of node template id --> image id --> eligible flavors for image and node
     */
    private Map<String, Map<String, List<CloudImageFlavor>>> flavorMatchResult;

    /**
     * Image match result contains couple of node template id --> eligible storage templates
     */
    private Map<String, List<StorageTemplate>> storageMatchResult;

    /**
     * Image match result contains couple of node template id --> eligible network templates
     */
    private Map<String, List<NetworkTemplate>> networkMatchResult;
}
