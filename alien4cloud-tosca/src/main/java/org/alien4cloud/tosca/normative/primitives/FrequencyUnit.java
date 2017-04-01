package org.alien4cloud.tosca.normative.primitives;

import lombok.Getter;

/**
 * TOSCA frequency units
 */
public enum FrequencyUnit implements Unit {
    HZ(1L), KHZ(1000L), MHZ(1000L * 1000L), GHZ(1000L * 1000L * 1000L);

    /**
     * Multiplier if convert to base unit which is byte (B)
     */
    @Getter
    private double multiplier;

    FrequencyUnit(double multiplier) {
        this.multiplier = multiplier;
    }
}
