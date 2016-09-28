package org.alien4cloud.tosca.model.definitions;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class ListPropertyValue extends PropertyValue<List<Object>> {

    public ListPropertyValue(List<Object> value) {
        super(value);
    }
}
