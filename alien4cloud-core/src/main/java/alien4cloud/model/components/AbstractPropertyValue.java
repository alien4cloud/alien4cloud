package alien4cloud.model.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Abstract class for a property value (can be implemented by a scalar value or a function value).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractPropertyValue implements IOperationParameter {
    @Override
    public boolean isDefinition() {
        return false;
    }

}