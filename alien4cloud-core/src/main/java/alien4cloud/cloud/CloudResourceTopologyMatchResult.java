package alien4cloud.cloud;

import java.util.Collection;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.AvailabilityZone;
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
     * This is the product of imageMatchResult and flavorMatchResult.
     * Compute template match result contains couple of node template id --> eligible compute templates
     */
    private Map<String, Collection<ComputeTemplate>> computeMatchResult;

    /**
     * Image match result contains couple of node template id --> eligible storage templates
     */
    private Map<String, Collection<StorageTemplate>> storageMatchResult;

    /**
     * Network match result contains couple of node template id --> eligible network templates
     */
    private Map<String, Collection<NetworkTemplate>> networkMatchResult;

    /**
     * Match group to a set of availability zone defined on the cloud
     */
    private Map<String, Collection<AvailabilityZone>> availabilityZoneMatchResult;
}
