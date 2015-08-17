package alien4cloud.model.components;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ComplexPropertyValue extends AbstractPropertyValue {
    private Map<String, Object> value;
}
