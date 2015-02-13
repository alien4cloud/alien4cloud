package alien4cloud.utils;

import java.util.Map;

import alien4cloud.model.cloud.ICloudResourceTemplate;

import com.google.common.collect.Maps;

public class MappingUtil {
    private MappingUtil() {
    }

    public static <U extends ICloudResourceTemplate> Map<String, U> getReverseMapping(Map<U, String> mapping) {
        Map<String, U> reverse = Maps.newHashMap();
        for (Map.Entry<U, String> mappingEntry : mapping.entrySet()) {
            reverse.put(mappingEntry.getValue(), mappingEntry.getKey());
        }
        return reverse;
    }

    public static <U extends ICloudResourceTemplate> Map<U, String> getMapping(Map<String, U> resources, Map<String, String> mapping) {
        Map<U, String> reverseResourceMapping = Maps.newHashMap();
        for (Map.Entry<String, String> mappingEntry : mapping.entrySet()) {
            String paaSResourceId = mappingEntry.getValue();
            U alienResource = resources.get(mappingEntry.getKey());
            reverseResourceMapping.put(alienResource, paaSResourceId);
        }
        return reverseResourceMapping;
    }
}
