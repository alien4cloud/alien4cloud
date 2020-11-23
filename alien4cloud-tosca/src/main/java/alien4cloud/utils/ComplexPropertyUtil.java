package alien4cloud.utils;

import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ComplexPropertyUtil {

    private ComplexPropertyUtil() {
    }

    public static void transform(ComplexPropertyValue property, UnaryOperator<Object> transformation) {
        transform(property.getValue(),transformation);
    }

    public static void transform(Map<String,Object> map, UnaryOperator<Object> transformation) {
        for (Map.Entry<String,Object> e : map.entrySet()) {
            Object oldValue = e.getValue();
            Object newValue = transformation.apply(oldValue);

            if (newValue != oldValue) {
                e.setValue(newValue);
            }

            if (newValue instanceof Map) {
                transform((Map) newValue,transformation);
            } else if (newValue instanceof List) {
                transform((List) newValue,transformation);
            }
        }
    }

    public static void transform(List<Object> list, UnaryOperator<Object> transformation) {
        for (int i = 0 ; i < list.size(); i ++) {
            Object oldValue = list.get(i);
            Object newValue = transformation.apply(oldValue);

            if (newValue != oldValue) {
                list.set(i,newValue);
            }

            if (newValue instanceof Map) {
                transform((Map) newValue,transformation);
            } else if (newValue instanceof List) {
                transform((List) newValue,transformation);
            }
        }
    }
}
