package alien4cloud.tosca.normative;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Minh Khang VU
 */
public abstract class ScalarType<T extends ScalarUnit<U>, U extends Unit> implements IComparablePropertyType<T> {

    private static final Pattern SIZE_PATTERN = Pattern.compile("^\\s*(\\d+)\\s+(\\p{Alnum}+)\\s*$");

    protected abstract T doParse(Long value, String unitText) throws InvalidPropertyValueException;

    @Override
    public T parse(String text) throws InvalidPropertyValueException {
        Matcher sizeMatcher = SIZE_PATTERN.matcher(text);
        if (sizeMatcher.matches()) {
            String valueText = sizeMatcher.group(1);
            String unitText = sizeMatcher.group(2);
            try {
                return doParse(Long.parseLong(valueText), unitText);
            } catch (NumberFormatException e) {
                throw new InvalidPropertyValueException("Could not parse scalar from value " + text + " as this is not a valid number " + valueText, e);
            }
        } else {
            throw new InvalidPropertyValueException("Could not parse scalar from value " + text + " as it does not match pattern " + SIZE_PATTERN);
        }
    }

    @Override
    public String print(T value) {
        return value.getValue() + " " + value.getUnit();
    }
}
