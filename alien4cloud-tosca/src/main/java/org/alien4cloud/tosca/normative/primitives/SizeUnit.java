package org.alien4cloud.tosca.normative.primitives;

import lombok.Getter;

/**
 * @author Minh Khang VU
 */
public enum SizeUnit implements Unit {

    B(1L),
    KB(1000L),
    KIB(1024),
    MB(1000L * 1000L),
    MIB(1024L * 1024L),
    GB(1000L * 1000L * 1000L),
    GIB(1024L * 1024L * 1024L),
    TB(1000L * 1000L * 1000L * 1000L),
    TIB(1024L * 1024L * 1024L * 1024L);

    /**
     * Multiplier if convert to base unit which is byte (B)
     */
    @Getter
    private double multiplier;

    SizeUnit(double multiplier) {
        this.multiplier = multiplier;
    }
}
