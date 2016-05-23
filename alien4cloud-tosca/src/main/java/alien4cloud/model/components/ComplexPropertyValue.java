package alien4cloud.model.components;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ComplexPropertyValue extends PropertyValue<Map<String, Object>> {

    public ComplexPropertyValue(Map<String, Object> value) {
        super(value);
    }
}
