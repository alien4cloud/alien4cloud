package alien4cloud.model.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Abstract class for a value that doesn't have a property definition (such as scalar value or a function value).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractPropertyValue implements IValue {
    @Override
    public boolean isDefinition() {
        return false;
    }
}