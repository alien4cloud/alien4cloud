package alien4cloud.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

public class SpringUtils {

    /**
     * Get beans of type from context and recursively from parent contexts.
     */
    public static <T extends Object> Collection<T> getBeansOfType(ApplicationContext context, Class<T> type) {
        List<T> result = new ArrayList<T>();
        recursivelyGetBeansOfType(context, type, result);
        return result;
    }

    private static <T extends Object> void recursivelyGetBeansOfType(ApplicationContext context, Class<T> type, List<T> result) {
        result.addAll(context.getBeansOfType(type).values());
        if (context.getParent() != null) {
            recursivelyGetBeansOfType(context.getParent(), type, result);
        }
    }

    /**
     * Check if a singleton bean is
     * 
     * @param object The object to check.
     * @param context The application context that may be the owner of the given object.
     * @return True if the object is a bean of the given context, false if not.
     */
    public static <T> boolean isSingletonOwnedByContex(ApplicationContext context, T object) {
        Map<String, T> contextInstances = context.getBeansOfType((Class<T>) object.getClass(), false, false);
        for (T contextInstace : contextInstances.values()) {
            if (contextInstace == object) {
                return true;
            }
        }
        return false;
    }
}
