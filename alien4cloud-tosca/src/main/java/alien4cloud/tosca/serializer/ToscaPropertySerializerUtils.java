package alien4cloud.tosca.serializer;

import alien4cloud.paas.exception.NotSupportedException;
import org.alien4cloud.tosca.model.definitions.*;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ToscaPropertySerializerUtils {

    private static Pattern ESCAPE_PATTERN = Pattern.compile(".*[,:\\\\\\[\\]\\{\\}-].*");
    private static Pattern VALID_YAML_PATTERN = Pattern.compile("[a-zA-Z0-9]+");
    private static Pattern FLOAT_PATTERN = Pattern.compile("([0-9]+[.])?[0-9]+");

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
            if (text == null) {
                text = "";
            } else if (!VALID_YAML_PATTERN.matcher(text).matches() && !FLOAT_PATTERN.matcher(text).matches()) {
                text = "\"" + escapeDoubleQuotedString(text) + "\"";
            }
            return text;
        }
    }

    public static String formatPropertyValue(int indentLevel, AbstractPropertyValue propertyValue) {
        return ToscaPropertySerializerUtils.formatPropertyValue(true, indentLevel, propertyValue);
    }

    private static String formatPropertyValue(boolean appendLf, int indentLevel, AbstractPropertyValue propertyValue) {
        if (propertyValue instanceof PropertyValue) {
            return formatValue(appendLf, indentLevel, ((PropertyValue) propertyValue).getValue());
        } else if (propertyValue instanceof FunctionPropertyValue) {
            return formatFunctionPropertyValue(indentLevel, ((FunctionPropertyValue) propertyValue));
        } else if (propertyValue instanceof ConcatPropertyValue) {
            return formatConcatPropertyValue(indentLevel, ((ConcatPropertyValue) propertyValue));
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
            return formatPropertyValue(indentLevel, (PropertyValue) value);
        } else {
            throw new NotSupportedException("Do not support other types than string map and list");
        }
    }

    private static String formatFunctionPropertyValue(int indentLevel, FunctionPropertyValue value) {
        indentLevel++;
        StringBuilder buffer = new StringBuilder();
        if (value.getFunction().equals("get_input")) {
            buffer.append("{ ").append(value.getFunction()).append(": ").append(value.getParameters().get(0)).append(" }");
        } else {
            buffer.append("{ ").append(value.getFunction()).append(": [").append(ToscaSerializerUtils.getCsvToString(value.getParameters())).append("] }");
        }
        return buffer.toString();
    }

    private static String formatConcatPropertyValue(int indentLevel, ConcatPropertyValue value) {
        indentLevel++;
        StringBuilder buffer = new StringBuilder().append("{ concat: [ ");

        boolean first = true;
        for (AbstractPropertyValue concatElement : value.getParameters()) {
            if (first) {
                first = false;
            } else {
                buffer.append(", ");
            }
            buffer.append(formatPropertyValue(0, concatElement));
        }

        buffer.append(" ] }");
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
        if (value.isEmpty()) {
            buffer.append("[]");
        } else {
            for (Object element : value) {
                if (element != null) {
                    buffer.append("\n").append(indent(indentLevel)).append("- ").append(formatValue(false, indentLevel, element));
                }
            }
        }
        return buffer.toString();
    }

    public static String formatProperties(int indentLevel, Map<String, ? extends AbstractPropertyValue> properties) {
        StringBuilder buffer = new StringBuilder();
        for (Map.Entry<String, ? extends AbstractPropertyValue> propertyEntry : properties.entrySet()) {
            if (propertyEntry.getValue() != null) {
                if (propertyEntry.getValue() instanceof PropertyValue && ((PropertyValue) propertyEntry.getValue()).getValue() == null) {
                    continue;
                }
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
            return "\"" + escapeDoubleQuotedString(scalar) + "\"";
        } else if (scalar.startsWith(" ") || scalar.endsWith(" ")) {
            return "\"" + escapeDoubleQuotedString(scalar) + "\"";
        } else {
            return scalar;
        }
    }

    public static String escapeDoubleQuotedString(String scalar) {
        return StringEscapeUtils.escapeJava(scalar);
    }

    public static String escapeDoubleQuote(String scalar) {
        if (scalar != null && scalar.contains("\"")) {
            // escape double quote
            return scalar.replaceAll("\"", "\\\\\"");
        }
        return scalar;
    }

    /**
     * Check if a property has been defined with a non null and not empty value.
     *
     * Note: used in cloudify 3 provider blueprint generator. Should it be moved ?
     *
     * @param properties The map of properties in which to look.
     * @param property The name of the property.
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

    private static boolean isPrimitiveType(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }
}
