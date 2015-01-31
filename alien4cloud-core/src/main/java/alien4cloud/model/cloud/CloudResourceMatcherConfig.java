package alien4cloud.model.cloud;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import com.google.common.collect.Maps;

@Getter
@Setter
public class CloudResourceMatcherConfig {

    private Map<CloudImage, String> imageMapping = Maps.newHashMap();

    private Map<CloudImageFlavor, String> flavorMapping = Maps.newHashMap();

    private Map<NetworkTemplate, String> networkMapping = Maps.newHashMap();

    private Map<StorageTemplate, String> storageMapping = Maps.newHashMap();
}