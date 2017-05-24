package org.alien4cloud.tosca.normative.types;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.normative.primitives.Size;
import org.alien4cloud.tosca.normative.primitives.SizeUnit;

public class SizeType extends ScalarType<Size, SizeUnit> {

    public static final String NAME = "scalar-unit.size";

    @Override
    protected Size doParse(Double value, String unitText) throws InvalidPropertyValueException {
        try {
            return new Size(value, SizeUnit.valueOf(unitText.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyValueException("Could not parse size scalar unit from value " + unitText, e);
        }
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}