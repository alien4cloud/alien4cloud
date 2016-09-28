package org.alien4cloud.tosca.model.definitions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.tosca.container.validation.ToscaPropertyConstraint;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueConstraints;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueType;
import alien4cloud.tosca.container.validation.ToscaPropertyPostValidationGroup;
import alien4cloud.tosca.container.validation.ToscaPropertyType;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormValidValues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@ToscaPropertyDefaultValueType
@ToscaPropertyConstraint
@ToscaPropertyDefaultValueConstraints(groups = { ToscaPropertyPostValidationGroup.class })
@JsonIgnoreProperties(ignoreUnknown = true)
@FormProperties({ "type", "default", "description" })
public class AttributeDefinition implements IValue {
    @ToscaPropertyType
    @FormValidValues({ "boolean", "string", "float", "integer", "version" })
    private String type;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String defaultValue;
    private String description;

    public String getDefault() {
        return this.defaultValue;
    }

    public void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isDefinition() {
        return true;
    }
}