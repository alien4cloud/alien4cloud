package alien4cloud.tosca.normative;

import lombok.Getter;

/**
 * @author Minh Khang VU
 */
public enum SizeUnit implements Unit {

    B(1L),
    kB(1000L),
    KiB(1024),
    MB(1000L * 1000L),
    MiB(1024L * 1024L),
    GB(1000L * 1000L * 1000L),
    GiB(1024L * 1024L * 1024L),
    TB(1000L * 1000L * 1000L * 1000L),
    TiB(1024L * 1024L * 1024L * 1024L);

    /**
     * Multiplier if convert to base unit which is byte (B)
     */
    @Getter
    private double multiplier;

    SizeUnit(double multiplier) {
        this.multiplier = multiplier;
    }
}
