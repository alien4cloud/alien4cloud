package alien4cloud.rest.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import alien4cloud.utils.ReflectionUtil;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Simple utility for patch management
 */
public final class PatchUtil {
    private static final String NULL = "null";

    /**
     * Update or Patch an instance.
     * 
     * @param instance The instance to patch.
     * @param fieldName The name of the property to patch.
     * @param value The value to use to Update (always change) or Patch the object. May be
     *            null => do not patch
     *            Object instance parsed from the "null" json string meaning set to null
     *            Or an actual object instance that represents the new value to set.
     * @param patch True if we should patch the instance, false if we should update.
     * @return True if the object has been updated, false if not.
     * @throws NoSuchFieldException In case the field does not exists.
     */
    public static boolean set(Object instance, String fieldName, Object value, boolean patch) {
        if (patch) {
            if (value == null) {
                return false;
            }
            if (RestMapper.NULL_INSTANCES.get(value.getClass()) == value) {
                value = null;
            }
        }
        try {
            Field field = ReflectionUtil.getDeclaredField(instance.getClass(), fieldName);
            if (patch) {
            }
            field.setAccessible(true);
            Object previousValue = field.get(instance);
            boolean update = previousValue == null ? value != null : !previousValue.equals(value);
            if (update) {
                field.set(instance, value);
            }
            return update;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to patch field <" + fieldName + ">");
        }
    }

    public static <A, B> Map<A, B> setMap(Map<A, B> instance, Map<A, B> newValues, boolean patch) {
        if (patch) {
            if (newValues == null || newValues.size() == 0) {
                return instance;
            }
        }

        if (instance == null) {
            instance = new HashMap<>();
        }

        if (patch) {
            for (Map.Entry<A, B> entry : newValues.entrySet()) {
                if (entry.getValue() == null || realValue(entry.getValue()) == null) {
                    // remove
                    instance.remove(entry.getKey());
                } else {
                    // add or update
                    instance.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            instance.clear();
            instance.putAll(safe(newValues));
        }

        return instance;
    }

    /**
     * Get the actual value that the user wanted to set.
     *
     * @param value The value that the user wanted to set.
     * @return Null if the user wanted to remove the actual value. Or the object value.
     */
    public static <T> T realValue(T value) {
        if (value == RestMapper.NULL_INSTANCES.get(value.getClass())) {
            return null;
        }
        return value;
    }
}
