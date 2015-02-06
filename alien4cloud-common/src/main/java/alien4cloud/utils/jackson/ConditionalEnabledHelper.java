package alien4cloud.utils.jackson;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DatabindContext;

/**
 * Utility to share the condition validation code between serializer and de-serializer.
 */
public class ConditionalEnabledHelper {
    /**
     * return the attributes defined on the conditional on attribute annotation.
     *
     * @param property The property from witch to extract the list of attributes to enable the conditional (de)serializer.
     * @return get the attributes
     */
    private static String[] getAttributes(BeanProperty property) {
        ConditionalOnAttribute conditionalOnAttribute = property.getAnnotation(ConditionalOnAttribute.class);
        if (conditionalOnAttribute != null) {
            return conditionalOnAttribute.value();
        }
        return new String[0];
    }

    /**
     * Checks if a conditional serializer or de-serializer should be enabled.
     *
     * @param context The serialization or de-serialization context.
     * @param property The property from witch to extract the list of attributes to enable the conditional (de)serializer.
     * @return true if the condition is matched (one of the attribute exists in context) or false if not.
     */
    public static boolean isEnabled(DatabindContext context, BeanProperty property) {
        String[] enabledAttributes = getAttributes(property);
        for (String attribute : enabledAttributes) {
            if (context.getAttribute(attribute) != null) {
                return true;
            }
        }
        return false;
    }
}
