package alien4cloud.tosca.serializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import alien4cloud.paas.exception.NotSupportedException;

public class ToscaPropertySerializerUtils {

    private static Pattern ESCAPE_PATTERN = Pattern.compile(".*[,:\\[\\]\\{\\}-].*");

    public static String indent(int indentLevel) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            buffer.append("  ");
        }
        return buffer.toString();
    }

    public static String formatTextValue(int indentLevel, String text) {
        if (text != null && text.contains("\n")) {
            indentLevel++;
            StringBuilder indentationBuffer = new StringBuilder();
            for (int i = 0; i < indentLevel; i++) {
                indentationBuffer.append("  ");
            }
            String indentation = indentationBuffer.toString();
            StringBuilder formattedTextBuffer = new StringBuilder("|\n");
            indentation += "  ";
            String[] lines = text.split("\n");
            for (String line : lines) {
                formattedTextBuffer.append(indentation).append(line).append("\n");
            }
            return formattedTextBuffer.toString();
        } else {
            return text == null ? "" : text;
        }
    }

    public static String formatPropertyValue(int indentLevel, AbstractPropertyValue propertyValue) {
        return ToscaPropertySerializerUtils.formatPropertyValue(true, indentLevel, propertyValue);
    }

    public static String formatPropertyValue(boolean appendLf, int indentLevel, AbstractPropertyValue propertyValue) {
        if (propertyValue instanceof PropertyValue) {
            return formatValue(appendLf, indentLevel, ((PropertyValue) propertyValue).getValue());
        } else if (propertyValue instanceof FunctionPropertyValue) {
            return formatFunctionPropertyValue(appendLf, indentLevel, ((FunctionPropertyValue) propertyValue));
        } else {
            throw new NotSupportedException("Do not support other types than PropertyValue or FunctionPropertyValue");
        }
    }

    private static String formatValue(int indentLevel, Object value) {
        return formatValue(true, indentLevel, value);
    }

    public static String formatValue(boolean appendLf, int indentLevel, Object value) {
        if (isPrimitiveType(value)) {
            return formatTextValue(indentLevel, (String) value);
        } else if (value instanceof Map) {
            return formatMapValue(appendLf, indentLevel, (Map<String, Object>) value);
        } else if (value instanceof Object[]) {
            return formatListValue(indentLevel, Arrays.asList((Object[]) value));
        } else if (value instanceof List) {
            return formatListValue(indentLevel, (List<Object>) value);
        } else if (value instanceof PropertyValue) {
            return formatPropertyValue(appendLf, indentLevel, (PropertyValue) value);
        } else {
            throw new NotSupportedException("Do not support other types than string map and list");
        }
    }

    private static String formatFunctionPropertyValue(boolean appendLf, int indentLevel, FunctionPropertyValue value) {
        indentLevel++;
        StringBuilder buffer = new StringBuilder();
        if (value.getFunction().equals("get_input")) {
            buffer.append("{ ").append(value.getFunction()).append(": ").append(value.getParameters().get(0)).append(" }");
        } else {
            buffer.append("{ ").append(value.getFunction()).append(": [").append(ToscaSerializerUtils.getCsvToString(value.getParameters())).append("] }");
        }
        return buffer.toString();
    }

    private static String formatMapValue(boolean appendFirstLf, int indentLevel, Map<String, Object> value) {
        indentLevel++;
        StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (Map.Entry<String, Object> valueEntry : value.entrySet()) {
            if (valueEntry.getValue() != null) {
                if (!isFirst || appendFirstLf) {
                    buffer.append("\n").append(indent(indentLevel));
                }
                buffer.append(valueEntry.getKey()).append(": ").append(formatValue(indentLevel, valueEntry.getValue()));
                if (isFirst) {
                    isFirst = false;
                }
            }
        }
        return buffer.toString();
    }

    private static String formatListValue(int indentLevel, List<Object> value) {
        indentLevel++;
        StringBuilder buffer = new StringBuilder();
        for (Object element : value) {
            if (element != null) {
                buffer.append("\n").append(indent(indentLevel)).append("- ").append(formatValue(false, indentLevel, element));
            }
        }
        return buffer.toString();
    }

    public static String formatProperties(int indentLevel, Map<String, AbstractPropertyValue> properties) {
        StringBuilder buffer = new StringBuilder();
        for (Map.Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {
            if (propertyEntry.getValue() != null) {
                buffer.append("\n").append(indent(indentLevel)).append(propertyEntry.getKey()).append(": ")
                        .append(formatPropertyValue(indentLevel, propertyEntry.getValue()));
            }
        }
        return buffer.toString();
    }

    /**
     * Render the scalar: when it contain '[' or ']' or '{' or '}' or ':' or '-' or ',', then quote the scalar.
     */
    public static String renderScalar(String scalar) {
        if (scalar == null) {
            return null;
        } else if (ESCAPE_PATTERN.matcher(scalar).matches()) {
            return "\"" + escapeDoubleQuote(scalar) + "\"";
        } else if (scalar.startsWith(" ") || scalar.endsWith(" ")) {
            return "\"" + escapeDoubleQuote(scalar) + "\"";
        } else {
            return scalar;
        }
    }

    private static String escapeDoubleQuote(String scalar) {
        if (scalar != null && scalar.contains("\"")) {
            // escape double quote
            return scalar.replaceAll("\"", "\\\\\"");
        }
        return scalar;
    }

    /**
     * Check if a property has been defined with a non null and not empty value.
     *
     * @param properties
     *            The map of properties in which to look.
     * @param property
     *            The name of the property.
     * @return True if a value has been defined, false if not.
     */
    public static boolean hasPropertyValue(Map<String, AbstractPropertyValue> properties, String property) {
        if (properties == null) {
            return false;
        }
        AbstractPropertyValue propertyValue = properties.get(property);
        if (propertyValue == null) {
            return false;
        }
        if (propertyValue instanceof ScalarPropertyValue) {
            String value = ((ScalarPropertyValue) propertyValue).getValue();
            if (value == null || value.isEmpty()) {
                return false;
            }
            // there is a non-null and not empty property value.
            return true;
        }
        return false;
    }

    public static Map<String, AbstractPropertyValue> addPropertyValueIfMissing(Map<String, AbstractPropertyValue> properties, String key, String value) {
        Map<String, AbstractPropertyValue> copy = new HashMap<>(properties);
        if (!copy.containsKey(key) || copy.get(key) == null) {
            copy.put(key, new ScalarPropertyValue(value));
        }
        return copy;
    }

    private static boolean isPrimitiveType(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }
}
