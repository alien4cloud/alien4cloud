package alien4cloud.model.common;

import javax.validation.constraints.NotNull;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.tosca.container.validation.ToscaPropertyConstraint;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueConstraints;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueType;
import alien4cloud.tosca.container.validation.ToscaPropertyPostValidationGroup;
import alien4cloud.ui.form.annotation.FormLabel;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import alien4cloud.ui.form.annotation.FormValidValues;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Predefined configuration for tag edit
 */
@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToscaPropertyDefaultValueType
@ToscaPropertyConstraint
@ToscaPropertyDefaultValueConstraints(groups = { ToscaPropertyPostValidationGroup.class })
@FormProperties({ "name", "description", "required", "filtered", "target", "type", "password", "default", "constraints" })
public class MetaPropConfiguration extends PropertyDefinition {
    /**
     * Auto generated id
     */
    @Id
    private String id;

    /**
     * The name of the tag
     */
    @TermFilter
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @NotNull
    @FormLabel("COMMON.NAME")
    private String name;

    /**
     * Target of the tag configuration (application or component or cloud)
     */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @FormValidValues({ MetaPropertyTarget.APPLICATION, MetaPropertyTarget.LOCATION, MetaPropertyTarget.COMPONENT, MetaPropertyTarget.SERVICE, MetaPropertyTarget.TOPOLOGY })
    @NotNull
    @TermsFacet
    @FormLabel("COMMON.TARGET")
    private String target;

    @JsonProperty("default")
    @JsonDeserialize(using = PropertyValueDeserializer.class)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private PropertyValue defaultValue;

    @NotNull
    @TermFilter
    @BooleanField
    @FormLabel("COMMON.INCLUDE_IN_FILTERS")
    private boolean filtered = false;

    @ObjectField(enabled = false)
    @FormPropertyDefinition(type = "string")
    public PropertyValue getDefault() {
        return this.defaultValue;
    }

    public void setDefault(PropertyValue defaultValue) {
        this.defaultValue = defaultValue;
    }

}
