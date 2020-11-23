package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

import alien4cloud.json.deserializer.ComplexPropertyValueDeserializer;
import alien4cloud.json.deserializer.PropertyValueDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class ComplexPropertyValue extends PropertyValue<Map<String, Object>> {

    @JsonDeserialize(using = ComplexPropertyValueDeserializer.class)
    protected Map<String,Object> value;

    public ComplexPropertyValue(Map<String, Object> value) {
        this.value = value;
    }
}
