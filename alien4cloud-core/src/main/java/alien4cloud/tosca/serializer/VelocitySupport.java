package alien4cloud.tosca.serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.components.constraints.AbstractPropertyConstraint;
import alien4cloud.model.components.constraints.EqualConstraint;
import alien4cloud.model.components.constraints.GreaterOrEqualConstraint;
import alien4cloud.model.components.constraints.GreaterThanConstraint;
import alien4cloud.model.components.constraints.InRangeConstraint;
import alien4cloud.model.components.constraints.LengthConstraint;
import alien4cloud.model.components.constraints.LessOrEqualConstraint;
import alien4cloud.model.components.constraints.LessThanConstraint;
import alien4cloud.model.components.constraints.MaxLengthConstraint;
import alien4cloud.model.components.constraints.MinLengthConstraint;
import alien4cloud.model.components.constraints.PatternConstraint;
import alien4cloud.model.components.constraints.ValidValuesConstraint;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;

public class VelocitySupport {

    private static Pattern ESCAPE_PATTERN = Pattern.compile(".*[:\\[\\]\\{\\}-].*");

    public boolean collectionIsNotEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }

    public boolean mapIsNotEmpty(Map<?, ?> m) {
        return m != null && !m.isEmpty();
    }

    /**
     * Render the scalar: when it contain '[' or ']' or '{' or '}' or ':' or '-', then quote the scalar.
     */
    public String renderScalar(String scalar) {
        if (ESCAPE_PATTERN.matcher(scalar).matches()) {
            return "\"" + escapeDoubleQuote(scalar) + "\"";
        } else if (scalar.startsWith(" ") || scalar.endsWith(" ")) {
            return "\"" + escapeDoubleQuote(scalar) + "\"";
        } else {
            return scalar;
        }
    }

    private static String escapeDoubleQuote(String scalar) {
        if (scalar.contains("\"")) {
            // escape double quote
            return scalar.replaceAll("\"", "\\\\\"");
        }
        return scalar;
    }

    /**
     * Render a description. If the string contain CRLF, then render a multiline literal preserving indentation.
     */
    public String renderDescription(String description, String identation) throws IOException {
        if (description.contains("\n")) {
            BufferedReader br = new BufferedReader(new StringReader(description));
            StringWriter sw = new StringWriter();
            sw.write("|");
            sw.write("\n");
            String line = br.readLine();
            boolean isFirst = true;
            while (line != null) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sw.write("\n");
                }
                sw.write(identation);
                sw.write(line);
                line = br.readLine();
            }
            return sw.toString();
        } else {
            return description;
        }
    }

    /**
     * Check if the map is not null, not empty and contains at least one not null value.
     */
    public boolean mapIsNotEmptyAndContainsNotnullValues(Map<?, ?> m) {
        if (mapIsNotEmpty(m)) {
            for (Object o : m.values()) {
                if (o != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isScalarPropertyValue(AbstractPropertyValue apv) {
        return apv instanceof ScalarPropertyValue;
    }

    public boolean isFunctionPropertyValue(AbstractPropertyValue apv) {
        return apv instanceof FunctionPropertyValue;
    }

    public String getCsvToString(List<?> list) {
        return getCsvToString(list, false);
    }

    private String getCsvToString(List<?> list, boolean renderScalar) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        if (list != null) {
            for (Object o : list) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(", ");
                }
                if (renderScalar) {
                    sb.append(renderScalar(o.toString()));
                } else {
                    sb.append(o.toString());
                }
            }
        }
        return sb.toString();
    }

    public boolean hasCapabilitiesContainingNotNullProperties(NodeTemplate nodeTemplate) {
        Map<String, Capability> capabilities = nodeTemplate.getCapabilities();
        if (capabilities == null || capabilities.isEmpty()) {
            return false;
        }
        for (Capability capability : capabilities.values()) {
            if (capability == null) {
                continue;
            }
            if (mapIsNotEmptyAndContainsNotnullValues(capability.getProperties())) {
                return true;
            }
        }
        return false;
    }

    public String renderConstraint(AbstractPropertyConstraint c) {
        StringBuilder builder = new StringBuilder();
        if (c instanceof GreaterOrEqualConstraint) {
            builder.append("greater_or_equal: ");
            builder.append(renderScalar(((GreaterOrEqualConstraint) c).getGreaterOrEqual()));
        } else if (c instanceof GreaterThanConstraint) {
            builder.append("greater_than: ");
            builder.append(renderScalar(((GreaterThanConstraint) c).getGreaterThan()));
        } else if (c instanceof LessOrEqualConstraint) {
            builder.append("less_or_equal: ");
            builder.append(renderScalar(((LessOrEqualConstraint) c).getLessOrEqual()));
        } else if (c instanceof LessThanConstraint) {
            builder.append("less_than: ");
            builder.append(renderScalar(((LessThanConstraint) c).getLessThan()));
        } else if (c instanceof LengthConstraint) {
            builder.append("length: ");
            builder.append(((LengthConstraint) c).getLength());
        } else if (c instanceof MaxLengthConstraint) {
            builder.append("max_length: ");
            builder.append(((MaxLengthConstraint) c).getMaxLength());
        } else if (c instanceof MinLengthConstraint) {
            builder.append("min_length: ");
            builder.append(((MinLengthConstraint) c).getMinLength());
        } else if (c instanceof PatternConstraint) {
            builder.append("pattern: ");
            builder.append(((PatternConstraint) c).getPattern());
        } else if (c instanceof EqualConstraint) {
            builder.append("equal: ");
            builder.append(renderScalar(((EqualConstraint) c).getEqual()));
        } else if (c instanceof InRangeConstraint) {
            builder.append("in_range: ");
            builder.append("[ ");
            builder.append(getCsvToString(((InRangeConstraint) c).getInRange(), true));
            builder.append(" ]");
        } else if (c instanceof ValidValuesConstraint) {
            builder.append("valid_values: ");
            builder.append("[ ");
            builder.append(getCsvToString(((ValidValuesConstraint) c).getValidValues(), true));
            builder.append(" ]");
        }
        return builder.toString();
    }

}
