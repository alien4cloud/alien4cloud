package alien4cloud.model.components;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@FormProperties({ "value" })
public class ScalarPropertyValue extends AbstractPropertyValue {
    private String value;
}