package org.alien4cloud.tosca.normative.types;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.normative.constants.RangeConstants;
import org.alien4cloud.tosca.normative.primitives.Range;

/**
 * Property type definition for the tosca primitive range type.
 */
public class RangeType implements IComparablePropertyType<Range> {
    public static final String NAME = "range";

    @Override
    public Range parse(String text) throws InvalidPropertyValueException {
        String[] values = text.split(",");
        if (values.length == 1) {
            return new Range(getLong(values[0]));
        } else if (values.length == 2) {
            return new Range(getLong(values[0]), getLong(values[1]));
        }
        return null;
    }

    private Long getLong(String value) throws InvalidPropertyValueException {
        try {
            return RangeConstants.UNBOUNDED.equalsIgnoreCase(value) ? Long.MAX_VALUE : Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new InvalidPropertyValueException("Cannot parse long value " + value, e);
        }
    }

    @Override
    public String print(Range value) {
        return value.toString();
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
