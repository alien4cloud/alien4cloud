package alien4cloud.dao;

import java.util.Map;

import com.google.common.collect.Maps;

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
        if (values == null) {
            filters.put(key, new String[] { null });
        } else {
            filters.put(key, values);
        }
        return filters;
    }

    /**
     * Add a simple filter with a single key to the given filter list only if the key does'nt exists already.
     *
     * @param filters The exiting map of filters in which to add fitlers
     * @param key The key for the filter.
     * @param values The value for the filter.
     * @return The filters map.
     */
    public static Map<String, String[]> singleKeyFilter(Map<String, String[]> filters, String key, String... values) {
        if (filters == null) {
            return singleKeyFilter(key, values);
        }
        if (!filters.containsKey(key)) {
            filters.put(key, values);
        }
        return filters;
    }

    /**
     * Get simple filter map with a single value per key based on key1, value1, key2, value2 etc.
     * 
     * @param keyValues The list of key1, value1, key2, value2 etc.
     * @return The filters map.
     */
    public static Map<String, String[]> fromKeyValueCouples(String... keyValues) {
        Map<String, String[]> filters = Maps.newHashMap();
        for (int i = 0; i < keyValues.length; i += 2) {
            singleKeyFilter(filters, keyValues[i], keyValues[i + 1]);
        }
        return filters;
    }

    /**
     * Add simple filter map with a single value per key based on key1, value1, key2, value2 etc.
     *
     * @param filters The exiting map of filters in which to add fitlers
     * @param keyValues The list of key1, value1, key2, value2 etc.
     * @return The filters map.
     */
    public static Map<String, String[]> fromKeyValueCouples(Map<String, String[]> filters, String... keyValues) {
        if (filters == null) {
            return fromKeyValueCouples(keyValues);
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            singleKeyFilter(filters, keyValues[i], keyValues[i + 1]);
        }
        return filters;
    }
}
