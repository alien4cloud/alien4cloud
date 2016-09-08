package alien4cloud.model.components;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public abstract class PropertyValue<T> extends AbstractPropertyValue {
    protected T value;
}
