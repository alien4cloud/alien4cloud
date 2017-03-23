package org.alien4cloud.tosca.normative.types;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;

/**
 * We currently support only list size constraints hence perform validation against a long value only.
 */
public class ListType implements IComparablePropertyType<Long> {
    public static final String NAME = "list";

    @Override
    public Long parse(String text) throws InvalidPropertyValueException {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new InvalidPropertyValueException("Cannot parse list size value " + text, e);
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
