package alien4cloud.model.components;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplexPropertyValue extends AbstractPropertyValue {
    private Map<String, Object> value;
}
