package org.alien4cloud.tosca.model.definitions;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public abstract class PropertyValue<T> extends AbstractPropertyValue {

    public PropertyValue() {
    }

    public abstract T getValue();

    public abstract void setValue(T t);
}
