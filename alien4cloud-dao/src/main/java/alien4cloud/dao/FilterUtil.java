package alien4cloud.dao;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Utility to build simple filters for ES mapping filtering.
 */
public final class FilterUtil {
    private FilterUtil() {
    }

    /**
     * Get a simple filter with a single key.
     *
     * @param key The key for the filter.
     * @param values The value for the filter.
     * @return The filters map.
     */
    public static Map<String, String[]> singleKeyFilter(String key, String... values) {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put(key, values);
        return filters;
    }

    /**
     * Get simple filter map with a single value per key based on key1, value1, key2, value2 etc.
     * 
     * @param keyValues The list of key1, value1, key2, value2 etc.
     * @return The filters map.
     */
    public static Map<String, String[]> kvCouples(String... keyValues) {
        Map<String, String[]> filters = Maps.newHashMap();
        for (int i = 0; i < keyValues.length; i += 2) {
            filters.put(keyValues[i], new String[] { keyValues[i + 1] });
        }
        return filters;
    }
}
