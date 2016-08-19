package alien4cloud.tosca.normative;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Minh Khang VU
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor(suppressConstructorProperties = true)
public abstract class ScalarUnit<T extends Unit> implements Comparable<ScalarUnit<T>> {

    private double value;

    private T unit;

    @Override
    public int compareTo(ScalarUnit<T> o) {
        BigDecimal thisValue = new BigDecimal(value).multiply(new BigDecimal(unit.getMultiplier()));
        BigDecimal otherValue = new BigDecimal(o.value).multiply(new BigDecimal(o.unit.getMultiplier()));
        return thisValue.compareTo(otherValue);
    }

    /**
     * Convert the value into the requested unit
     * 
     * @param requestedUnitStr
     *            The requested unit as a String.
     * @return The value converted in the requested unit.
     */
    public double convert(String requestedUnitStr) {
        T requestedUnit = getUnit(requestedUnitStr);
        return (value * unit.getMultiplier() / requestedUnit.getMultiplier());
    }

    protected abstract T getUnit(String unit);
}
