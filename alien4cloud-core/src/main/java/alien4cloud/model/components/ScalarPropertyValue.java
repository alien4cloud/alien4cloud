package alien4cloud.model.components;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.ui.form.annotation.FormProperties;

/**
 * Represents a simple scalar property value.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FormProperties({ "value" })
public class ScalarPropertyValue extends PropertyValue<String> {

    public ScalarPropertyValue(String value) {
        super(value);
    }
}