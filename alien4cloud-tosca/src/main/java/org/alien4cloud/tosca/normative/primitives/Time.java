package org.alien4cloud.tosca.normative.primitives;

/**
 * @author Minh Khang VU
 */
public class Time extends ScalarUnit<TimeUnit> {

    public Time(double value, TimeUnit unit) {
        super(value, unit);
    }

    @Override
    protected TimeUnit getUnit(String unit) {
        return TimeUnit.valueOf(unit.toUpperCase());
    }
}
