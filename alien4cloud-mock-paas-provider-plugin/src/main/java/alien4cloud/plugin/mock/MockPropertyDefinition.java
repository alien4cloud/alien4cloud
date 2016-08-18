package alien4cloud.plugin.mock;

import lombok.*;

import org.elasticsearch.annotation.ObjectField;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.PropertyValue;
import alien4cloud.tosca.container.validation.ToscaPropertyConstraint;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueConstraints;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueType;
import alien4cloud.tosca.container.validation.ToscaPropertyPostValidationGroup;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Predefined configuration for tag edit
 */
@Getter
@Setter
@NoArgsConstructor
@ToscaPropertyDefaultValueType
@ToscaPropertyConstraint
@ToscaPropertyDefaultValueConstraints(groups = { ToscaPropertyPostValidationGroup.class })
@JsonIgnoreProperties(ignoreUnknown = true)
@FormProperties({ "type", "required", "default", "description" })
@ToString
public class MockPropertyDefinition extends PropertyDefinition {



    @JsonProperty("default")
    @JsonDeserialize(using = PropertyValueDeserializer.class)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private PropertyValue defaultValue;

    @ObjectField(enabled = false)
    @FormPropertyDefinition(type = "string")
    public PropertyValue getDefault() {
        return this.defaultValue;
    }

    public void setDefault(PropertyValue defaultValue) {
        this.defaultValue = defaultValue;
    }

}
