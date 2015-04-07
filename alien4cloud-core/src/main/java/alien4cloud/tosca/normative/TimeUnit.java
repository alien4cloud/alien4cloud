package alien4cloud.tosca.normative;

import lombok.Getter;

/**
 * @author Minh Khang VU
 */
public enum TimeUnit implements Unit {

    d(60 * 60 * 24), h(60 * 60), m(60), s(1), ms(Math.pow(10, -3)), us(Math.pow(10, -6)), ns(Math.pow(10, -9));

    /**
     * Multiplier if convert to base unit which is second (s)
     */
    @Getter
    private double multiplier;

    TimeUnit(double multiplier) {
        this.multiplier = multiplier;
    }
}
