package alien4cloud.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Maps;

public final class AlienUtils {

    public static final String DEFAULT_PREFIX_SEPARATOR = "_";
    public static final String COLON_SEPARATOR = ":";

    private AlienUtils() {

    }

    public static String putValueCommaSeparatedInPosition(String values, String valueToPut, int position) {
        String[] valuesArray;
        String separator = ",";
        int positionInArray = position - 1;
        if (StringUtils.isBlank(values)) {
            valuesArray = new String[0];
        } else {
            valuesArray = values.split(separator);
        }
        StringBuilder toReturnBuilder = new StringBuilder("");
        if (valuesArray.length >= position) {
            valuesArray[positionInArray] = valueToPut;
            String separatorToAppend;
            for (int i = 0; i < valuesArray.length; i++) {
                separatorToAppend = i + 1 == valuesArray.length ? "" : ",";
                toReturnBuilder.append(valuesArray[i]).append(separatorToAppend);
            }
        } else {
            int nbSeparatorToAdd = 0;
            if (StringUtils.isBlank(values)) {
                toReturnBuilder = new StringBuilder("");
                nbSeparatorToAdd = position - 1;
            } else {
                toReturnBuilder = new StringBuilder(values);
                nbSeparatorToAdd = position - valuesArray.length;
            }
            for (int i = 0; i < nbSeparatorToAdd; i++) {
                toReturnBuilder.append(separator);
            }
            toReturnBuilder.append(valueToPut);
        }
        return toReturnBuilder.toString();
    }

    /**
     * prefix a string with another
     *
     * @param separator
     * @param toPrefix
     * @param prefixes
     * @return
     */
    public static String prefixWith(String separator, String toPrefix, String... prefixes) {
        if (toPrefix == null) {
            return null;
        }
        if (ArrayUtils.isEmpty(prefixes)) {
            return toPrefix;
        }
        String finalSeparator = separator == null ? DEFAULT_PREFIX_SEPARATOR : separator;
        StringBuilder builder = new StringBuilder();
        for (String prefix : prefixes) {
            builder.append(prefix).append(finalSeparator);
        }
        return builder.append(toPrefix).toString();
    }

    /**
     * prefix a string with another using the default separator "_"
     *
     * @param toPrefix
     * @param prefixes
     * @return
     */
    public static String prefixWithDefaultSeparator(String toPrefix, String... prefixes) {
        return prefixWith(DEFAULT_PREFIX_SEPARATOR, toPrefix, prefixes);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> fromListToMap(List<V> list, String keyProperty, boolean useGetter) throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (list == null || StringUtils.isBlank(keyProperty)) {
            return null;
        }

        Map<K, V> map = Maps.newHashMap();

        if (useGetter) {
            for (V item : list) {
                Method[] methods = item.getClass().getMethods();
                for (Method method : methods) {
                    String getterName = "get" + keyProperty.toLowerCase();
                    if (method.getName().toLowerCase().equals(getterName)) {
                        K key = (K) method.invoke(item);
                        if (key != null) {
                            map.put(key, item);
                        }
                        break;
                    }
                }
            }

        } else {
            for (V item : list) {
                Field[] fields = item.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.getName().equals(keyProperty)) {
                        K key = (K) field.get(item);
                        if (key != null) {
                            map.put(key, item);
                        }
                        break;
                    }
                }
            }
        }

        return map;
    }
}
