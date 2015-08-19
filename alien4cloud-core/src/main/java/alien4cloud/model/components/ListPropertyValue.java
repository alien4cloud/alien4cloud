package alien4cloud.model.components;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ListPropertyValue extends PropertyValue<List<Object>> {

    public ListPropertyValue(List<Object> value) {
        super(value);
    }
}
