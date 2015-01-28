package alien4cloud.model.cloud;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudResourceMatcherConfig {

    @Id
    private String id;

    private List<MatchedNetworkTemplate> matchedNetworks = Lists.newArrayList();

    private List<MatchedCloudImage> matchedImages = Lists.newArrayList();

    private List<MatchedCloudImageFlavor> matchedFlavors = Lists.newArrayList();

    private List<MatchedCloudImageFlavor> matchedBlockStorages = Lists.newArrayList();

    @JsonIgnore
    public Map<NetworkTemplate, String> getNetworkMapping() {
        return getMapping(matchedNetworks);
    }

    @JsonIgnore
    public Map<CloudImage, String> getCloudImageMapping() {
        return getMapping(matchedImages);
    }

    @JsonIgnore
    public Map<CloudImageFlavor, String> getCloudImageFlavorMapping() {
        return getMapping(matchedFlavors);
    }

    @JsonIgnore
    public Map<CloudImageFlavor, String> getBlockStoragesMapping() {
        return getMapping(matchedBlockStorages);
    }

    private <T extends AbstractMatchedResource<U>, U extends ICloudResourceTemplate> Map<U, String> getMapping(List<T> matchedResources) {
        Map<U, String> config = Maps.newHashMap();
        if (matchedResources != null && !matchedResources.isEmpty()) {
            for (T network : matchedResources) {
                config.put(network.getResource(), network.getPaaSResourceId());
            }
        }
        return config;
    }
}