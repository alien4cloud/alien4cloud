package alien4cloud.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.rest.utils.JsonUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;

/**
 * Utility class to ease map manipulation.
 */
public final class MapUtil {
    private MapUtil() {
    }

    /**
     * Converts an array of values into a map based on the given extractor function to get the key out of the objects.
     * 
     * @param values The values of the map.
     * @param keyExtractor The function to extract a key from values.
     * @param <K> The key type.
     * @param <V> The value type.
     * @return A new hashmap that contains all values associated with their keys.
     */
    public static <K, V> Map<K, V> newHashMap(V[] values, Function<V, K> keyExtractor) {
        Map<K, V> map = Maps.newHashMap();
        if (values != null) {
            for (V value : values) {
                map.put(keyExtractor.apply(value), value);
            }
        }
        return map;
    }

    /**
     * Map.putall with non null check on the other map.
     * 
     * @param map The map in which to put data.
     * @param otherMap The map to merge in the first map (may be null).
     * @param <K> Type of keys.
     * @param <V> Type of values.
     */
    public static <K, V> void putAll(Map<K, V> map, Map<K, V> otherMap) {
        if (otherMap != null) {
            map.putAll(otherMap);
        }
    }

    /**
     * This method allows to add an element to the list of elements in a map for a given key. If the list is null for the given key it will create a new list,
     * insert it in the map and add the element.
     * 
     * @param listMap A map of <Key, List<Values>>
     * @param key The key in the map.
     * @param value The element to add to the list that is mapped by the given key.
     * @param <K> The key class.
     * @param <V> The class of the list elements.
     */
    public static <K, V> void addToList(Map<K, List<V>> listMap, K key, V value) {
        List<V> list = listMap.get(key);
        if (list == null) {
            list = new ArrayList<V>();
            listMap.put(key, list);
        }
        list.add(value);
    }

    /**
     * Try to get a value following a path in a map or a list/array. For example :
     * MapUtil.get(map, "a.b.c") correspond to:
     * map.get(a).get(b).get(c)
     * MapUtil.get(list, "1.b.c.2") correspond to:
     * list[1].get(b).get(c)[2]
     *
     * @param object the map/list to search for path
     * @param path keys in the map separated by '.'
     */
    public static Object get(Object object, String path) {
        if (StringUtils.isBlank(path)) {
            throw new InvalidArgumentException("Path is empty, cannot evaluate property for object " + object);
        }
        String[] tokens = path.split("[\\.\\]\\[]");

        return get(object, tokens);
    }

    /**
     * Try to get a value following a path of tokens in a map or a list/array.
     *
     * @param object the map/list to search for path
     * @param tokens keys in the map
     */
    public static Object get(Object object, String... tokens) {
        Object value = object;
        for (String token : tokens) {
            if (StringUtils.isEmpty(token)) {
                continue;
            }
            if (value instanceof List) {
                List<Object> nested = (List<Object>) value;
                try {
                    int index = Integer.parseInt(token);
                    value = nested.get(index);
                    if (value == null) {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            } else if (value instanceof Object[]) {
                Object[] nested = (Object[]) value;
                try {
                    int index = Integer.parseInt(token);
                    value = nested[index];
                    if (value == null) {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                value = nested.get(token);
                if (value == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return value;
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

    /**
     * Revert a map key --> value become value --> key
     * 
     * @param map the map to revert
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return reverted map
     */
    public static <K, V> Map<V, K> revert(Map<K, V> map) {
        Map<V, K> reverted = new HashMap<V, K>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            reverted.put(entry.getValue(), entry.getKey());
        }
        return reverted;
    }

    /**
     * Remove the entry with 'oldKey' and put it (if exists) using 'newKey' unless if an entry already exist for 'newKey'.
     */
    public static <K, V> void replaceKey(Map<K, V> map, K oldKey, K newKey) {
        if (map == null || map.isEmpty()) {
            return;
        }
        if (map.containsKey(newKey)) {
            return;
        }
        V o = map.remove(oldKey);
        if (o != null) {
            map.put(newKey, o);
        }
    }

    public static Map<String, String> toString(Map<String, Object> stringObjectMap) throws JsonProcessingException {
        Map<String, String> stringStringMap = Maps.newHashMap();
        for (Map.Entry<String, Object> stringObjectEntry : stringObjectMap.entrySet()) {
            if (stringObjectEntry.getValue() != null) {
                if (stringObjectEntry.getValue() instanceof String) {
                    stringStringMap.put(stringObjectEntry.getKey(), (String) stringObjectEntry.getValue());
                } else {
                    stringStringMap.put(stringObjectEntry.getKey(), JsonUtil.toString(stringObjectEntry.getValue()));
                }
            } else {
                stringStringMap.put(stringObjectEntry.getKey(), null);
            }

        }
        return stringStringMap;
    }
}