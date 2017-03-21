package org.alien4cloud.tosca.normative.primitives;

/**
 * Range type implementation.
 */
public class Range implements Comparable<Range> {
    private Long min;
    private Long max;

    /**
     * Create a single value range for constraint management.
     * 
     * @param min The single value.
     */
    public Range(Long min) {
        this.min = min;
    }

    public Range(Long min, Long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public int compareTo(Range o) {
        if (o.max == null) {
            // Compare a range against single numeric value. We actually perform the compare the other way around.
            int compare = min.compareTo(o.min);
            if (compare < 0) {
                return -1;
            }
            compare = max.compareTo(o.min);
            if (compare > 0) {
                return compare;
            }
            return 0; // The provided number is within the range just consider it valid.
        } else if (this.max == null) {
            int compare = min.compareTo(o.min);
            if (compare < 0) {
                return -1;
            }
            compare = min.compareTo(o.max);
            if (compare > 0) {
                return compare;
            }
            return 0;
        }
        // Right now there is no need in alien to compare a range against another full range. To be implemented if required.
        return 0;
    }
}
