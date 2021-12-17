package org.alien4cloud.tosca.model.definitions;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.ui.form.annotation.FormProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@FormProperties({ "function_token", "parameters" })
public class TokenPropertyValue extends AbstractPropertyValue {
    private String function_token;
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private List<AbstractPropertyValue> parameters;
}
