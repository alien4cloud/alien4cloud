package org.alien4cloud.tosca.model.definitions;

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
