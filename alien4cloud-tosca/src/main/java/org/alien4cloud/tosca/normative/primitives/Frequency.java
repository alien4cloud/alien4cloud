package org.alien4cloud.tosca.normative.primitives;

/**
 * Scalar unit value for frequency.
 */
public class Frequency extends ScalarUnit<FrequencyUnit> {

    public Frequency(double value, FrequencyUnit unit) {
        super(value, unit);
    }

    @Override
    protected FrequencyUnit getUnit(String unit) {
        return FrequencyUnit.valueOf(unit.toUpperCase());
    }
}
