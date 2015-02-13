package alien4cloud.cloud;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.ComputeTemplate;
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
     * Images that are used compute templates
     */
    private Map<String, CloudImage> images;

    /**
     * Flavors that are used compute templates
     */
    private Map<String, CloudImageFlavor> flavors;

    /**
     * This is the product of imageMatchResult and flavorMatchResult
     */
    private Map<String, List<ComputeTemplate>> computeMatchResult;

    /**
     * Image match result contains couple of node template id --> eligible storage templates
     */
    private Map<String, List<StorageTemplate>> storageMatchResult;

    /**
     * Image match result contains couple of node template id --> eligible network templates
     */
    private Map<String, List<NetworkTemplate>> networkMatchResult;
}
