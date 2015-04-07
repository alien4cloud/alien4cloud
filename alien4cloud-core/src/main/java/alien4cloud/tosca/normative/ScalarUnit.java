package alien4cloud.tosca.normative;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Minh Khang VU
 */
@Getter
@Setter
@AllArgsConstructor
public abstract class ScalarUnit<T extends Unit> implements Comparable<ScalarUnit<T>> {

    private long value;

    private T unit;

    @Override
    public int compareTo(ScalarUnit<T> o) {
        Double thisValue = ((double) value * unit.getMultiplier());
        Double otherValue = ((double) value * unit.getMultiplier());
        return thisValue.compareTo(otherValue);
    }
}
