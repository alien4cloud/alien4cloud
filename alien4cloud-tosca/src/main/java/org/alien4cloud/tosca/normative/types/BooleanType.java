package org.alien4cloud.tosca.normative.types;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;

/**
 * @author Minh Khang VU
 */
public class BooleanType implements IPropertyType<Boolean> {
    public static final String NAME = "boolean";

    @Override
    public Boolean parse(String text) throws InvalidPropertyValueException {
        if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
            throw new InvalidPropertyValueException(text + " is not a valid boolean value");
        }
        return Boolean.parseBoolean(text);
    }

    @Override
    public String print(Boolean value) {
        return String.valueOf(value);
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
