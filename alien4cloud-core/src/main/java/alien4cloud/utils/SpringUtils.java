package alien4cloud.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

}
