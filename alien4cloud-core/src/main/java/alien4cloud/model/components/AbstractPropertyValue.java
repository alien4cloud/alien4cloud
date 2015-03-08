package alien4cloud.model.components;

import alien4cloud.json.deserializer.PropertyValueDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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