package alien4cloud.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class CollectionUtils {
    private CollectionUtils() {
    }

    /**
     * Add the content of the 'source' Set to the 'target' set and return the union set.
     * 
     * If 'source' is null then a new set is created and returned.
     * If 'target' is null then no content is added to the 'source' Set or newly created set.
     * 
     * @param source The Set to merge in the target Set.
     * @param target The Set in which the source set will be merged (through addAll).
     * @return The target Set with addition of source Set elements, or a new Set (including content of source set) if target was null.
     */
    public static <T> Set<T> merge(Set<T> source, Set<T> target) {
        Set<T> merged = Sets.newHashSet();
        if (target != null) {
            merged.addAll(target);
        }
        if (source != null) {
            merged.addAll(source);
        }
        return merged.isEmpty() ? null : merged;
    }

    /**
     * <p>
     * Add the content of the 'source' Map to the 'target' set and return the union Map.
     * </p>
     * <p>
     * If 'source' is null then a new Map is created and returned. If 'target' is null then no content is added to the 'source' Map or newly created Map.
     * </p>
     * 
     * @param source The Map to merge in the target Map.
     * @param target The Map in which the source Map will be merged (through addAll).
     * @param override If an key from the source map already exists in the target map, should it override (true) or not (false) the value.
     * @return The target Map with addition of source Map elements, or a new Map (including content of source set) if target was null.
     */
    public static <T, V> Map<T, V> merge(Map<T, V> source, Map<T, V> target, boolean override) {
        if (target == null) {
            target = Maps.newHashMap();
        }

        if (source != null) {
            for (Entry<T, V> entry : source.entrySet()) {
                if (override || !target.containsKey(entry.getKey())) {
                    target.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return target.isEmpty() ? null : target;
    }
}
