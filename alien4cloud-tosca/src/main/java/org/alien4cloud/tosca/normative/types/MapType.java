package org.alien4cloud.tosca.normative.types;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;

/**
 * We currently support only map size constraints hence perform validation against a long value only.
 */
public class MapType implements IComparablePropertyType<Long> {
    public static final String NAME = "map";

    @Override
    public Long parse(String text) throws InvalidPropertyValueException {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new InvalidPropertyValueException("Cannot parse map size value " + text, e);
        }
    }

    @Override
    public String print(Long value) {
        return String.valueOf(value);
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
