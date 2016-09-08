package alien4cloud.model.components;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class ComplexPropertyValue extends PropertyValue<Map<String, Object>> {

    public ComplexPropertyValue(Map<String, Object> value) {
        super(value);
    }
}
