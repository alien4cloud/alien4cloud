package org.alien4cloud.tosca.normative.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.normative.primitives.ScalarUnit;
import org.alien4cloud.tosca.normative.primitives.Unit;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract type for all scalar type with unit
 */
public abstract class ScalarType<T extends ScalarUnit<U>, U extends Unit> implements IComparablePropertyType<T> {
    private static final Pattern SCALAR_UNIT_PATTERN = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s+(\\p{Alnum}+)\\s*$");

    protected abstract T doParse(Double value, String unitText) throws InvalidPropertyValueException;

    @Override
    public T parse(String text) throws InvalidPropertyValueException {
        if (StringUtils.isEmpty(text)) {
            throw new InvalidPropertyValueException("Could not parse scalar from value " + text + " as the text is empty");
        }
        Matcher matcher = SCALAR_UNIT_PATTERN.matcher(text);
        if (matcher.matches()) {
            String valueText = matcher.group(1);
            String unitText = matcher.group(2);
            try {
                return doParse(Double.parseDouble(valueText), unitText);
            } catch (NumberFormatException e) {
                throw new InvalidPropertyValueException("Could not parse scalar from value " + text + " as this is not a valid number " + valueText, e);
            }
        } else {
            throw new InvalidPropertyValueException("Could not parse scalar from value " + text + " as it does not match pattern " + SCALAR_UNIT_PATTERN);
        }
    }

    @Override
    public String print(T value) {
        return value.getValue() + " " + value.getUnit();
    }
}
