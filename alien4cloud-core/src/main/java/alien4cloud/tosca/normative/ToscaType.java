package alien4cloud.tosca.normative;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;

import alien4cloud.utils.VersionUtil;

/**
 * The primitive type that TOSCA YAML supports.
 * 
 * @author mkv
 */
public enum ToscaType {
    STRING, INTEGER, FLOAT, BOOLEAN, TIMESTAMP, VERSION;

    // private static final Pattern TIMESTAMP_REGEX = Pattern
    // .compile("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]|[0-9][0-9][0-9][0-9]-[0-9][0-9]?-[0-9][0-9]?([Tt]|[ \\t]+)[0-9][0-9]?:[0-9][0-9]:[0-9][0-9](\\.[0-9]*)?(([ \\t]*)Z|([ \\t]*)[-+][0-9][0-9]?(:[0-9][0-9])?)?");

    public static ToscaType fromYamlTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        try {
            return ToscaType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isValidValue(String value) {
        switch (this) {
        case BOOLEAN:
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
        case FLOAT:
            return isFloat(value);
        case INTEGER:
            return isInteger(value);
        case STRING:
            return true;
        case TIMESTAMP:
            try {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).parse(value);
                return true;
            } catch (ParseException e) {
                return false;
            }
        case VERSION:
            return VersionUtil.isValid(value);
        default:
            return false;
        }
    }

    private boolean isFloat(String value) {
        try {
            Float.valueOf(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private boolean isInteger(String value) {
        try {
            Long.valueOf(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public Object convert(String value) {
        switch (this) {
        case STRING:
            return value;
        case BOOLEAN:
            return Boolean.valueOf(value);
        case FLOAT:
            return Double.valueOf(value);
        case INTEGER:
            return Long.valueOf(value);
        case TIMESTAMP:
            try {
                return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).parse(value);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Value must be a valid timestamp", e);
            }
        case VERSION:
            return VersionUtil.parseVersion(value);
        default:
            return null;
        }
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}