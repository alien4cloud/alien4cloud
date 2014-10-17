package alien4cloud.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to ease map manipulation.
 */
public final class MapUtil {
    private MapUtil() {
    }

    /**
     * Try to get a value following a path in the map. For example :
     * MapUtil.get(map, "a.b.c") correspond to:
     * map.get(a).get(b).get(c)
     * 
     * @param map the map to search for path
     * @param path keys in the map separated by '.'
     */
    public static Object get(Map<String, ? extends Object> map, String path) {
        String[] tokens = path.split("\\.");
        if (tokens.length == 0) {
            return null;
        } else {
            Object value = map;
            for (String token : tokens) {
                if (!(value instanceof Map)) {
                    return null;
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nested = (Map<String, Object>) value;
                    if (nested.containsKey(token)) {
                        value = nested.get(token);
                    } else {
                        return null;
                    }
                }
            }
            return value;
        }
    }

    /**
     * Create a new hash map and fills it from the given keys and values (keys[index] -> values[index].
     * 
     * @param keys The array of keys.
     * @param values The array of values.
     * @return A map that contains for each key element in the keys array a value from the values array at the same index.
     */
    public static <K, V> Map<K, V> newHashMap(K[] keys, V[] values) {
        Map<K, V> map = new HashMap<K, V>();
        if (keys == null || values == null || keys.length != values.length) {
            throw new IllegalArgumentException("keys and values must be non-null and have the same size.");
        }
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }
}