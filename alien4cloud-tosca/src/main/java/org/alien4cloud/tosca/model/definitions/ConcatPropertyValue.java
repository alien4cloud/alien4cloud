package org.alien4cloud.tosca.model.definitions;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.json.deserializer.OperationParameterDeserializer;
import alien4cloud.ui.form.annotation.FormProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@FormProperties({ "function_concat", "parameters" })
public class ConcatPropertyValue extends AbstractPropertyValue {
    private String function_concat;
    @JsonDeserialize(contentUsing = OperationParameterDeserializer.class)
    private List<IValue> parameters;
}
