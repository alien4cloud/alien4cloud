package alien4cloud.model.components;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a simple scalar property value.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FormProperties({ "value" })
@ToString(callSuper = true)
public class ScalarPropertyValue extends PropertyValue<String> {

    public ScalarPropertyValue(String value) {
        super(value);
    }
}