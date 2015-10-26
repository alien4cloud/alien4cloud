package alien4cloud.utils;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;

public class TypeMap {
    private Map<Class<?>, Map<String, Object>> cacheMap = Maps.newHashMap();

    private Map<String, Object> getMap(Class<?> clazz) {
        Map<String, Object> map = cacheMap.get(clazz);
        if (map == null) {
            cacheMap.put(clazz, new HashMap<String, Object>());
        }
        return cacheMap.get(clazz);
    }

    /**
     * put an object (value) in it's type map using the given key.
     * 
     * @param key
     *            The key inside the type map.
     * @param value
     *            The object to insert (based on it's type and the given key).
     */
    public void put(String key, Object value) {
        getMap(value.getClass()).put(key, value);
    }

    /**
     * Get the cached object based on it's type and key.
     * 
     * @param clazz
     *            The object's type.
     * @param key
     *            The object key.
     * @return The object that match the given type and key or null if none matches.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, String key) {
        return (T) (cacheMap.get(clazz) == null ? null : cacheMap.get(clazz).get(key));
    }
}