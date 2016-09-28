package org.alien4cloud.tosca.model.definitions;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import alien4cloud.model.components.IncompatiblePropertyDefinitionException;
import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.json.deserializer.PropertyConstraintDeserializer;
import alien4cloud.json.deserializer.PropertyValueDeserializer;
import org.alien4cloud.tosca.model.definitions.constraints.*;
import alien4cloud.tosca.container.validation.*;
import alien4cloud.ui.form.annotation.FormContentTypes;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormType;
import alien4cloud.ui.form.annotation.FormValidValues;
import lombok.*;

/**
 *
 * Only parameter exposed as property definitions can be used for "custom" operations.
 *
 * @author 'Igor Ngouagna'
 *
 */

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "type", "required", "description", "defaultValue", "constraints", "entrySchema" })
@ToscaPropertyDefaultValueType
@ToscaPropertyConstraint
@ToscaPropertyDefaultValueConstraints(groups = { ToscaPropertyPostValidationGroup.class })
@JsonIgnoreProperties(ignoreUnknown = true)
@FormProperties({ "type", "required", "default", "description" })
@ToString
public class PropertyDefinition implements IValue {
    @StringField(indexType = IndexType.not_analyzed)
    @ToscaPropertyType
    @FormValidValues({ "boolean", "string", "float", "integer", "version" })
    @NotNull
    private String type;

    @ObjectField(enabled = false)
    private PropertyDefinition entrySchema;

    @NotNull
    @BooleanField(index = IndexType.no)
    private boolean required = true;

    @JsonProperty("default")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private PropertyValue defaultValue;

    @StringField(indexType = IndexType.no)
    private String description;

    @StringField(indexType = IndexType.no)
    private String suggestionId;

    @ObjectField(enabled = false)
    @Valid
    @ToscaPropertyConstraintDuplicate
    @JsonDeserialize(contentUsing = PropertyConstraintDeserializer.class)
    @FormContentTypes({ @FormType(discriminantProperty = "equal", label = "CONSTRAINT.EQUAL", implementation = EqualConstraint.class),
            @FormType(discriminantProperty = "greaterThan", label = "CONSTRAINT.GREATER_THAN", implementation = GreaterThanConstraint.class),
            @FormType(discriminantProperty = "greaterOrEqual", label = "CONSTRAINT.GREATER_OR_EQUAL", implementation = GreaterOrEqualConstraint.class),
            @FormType(discriminantProperty = "lessThan", label = "CONSTRAINT.LESS_THAN", implementation = LessThanConstraint.class),
            @FormType(discriminantProperty = "lessOrEqual", label = "CONSTRAINT.LESS_OR_EQUAL", implementation = LessOrEqualConstraint.class),
            @FormType(discriminantProperty = "inRange", label = "CONSTRAINT.IN_RANGE", implementation = InRangeConstraint.class),
            @FormType(discriminantProperty = "length", label = "CONSTRAINT.LENGTH", implementation = LengthConstraint.class),
            @FormType(discriminantProperty = "maxLength", label = "CONSTRAINT.MAX_LENGTH", implementation = MaxLengthConstraint.class),
            @FormType(discriminantProperty = "minLength", label = "CONSTRAINT.MIN_LENGTH", implementation = MinLengthConstraint.class),
            @FormType(discriminantProperty = "pattern", label = "CONSTRAINT.PATTERN", implementation = PatternConstraint.class),
            @FormType(discriminantProperty = "validValues", label = "CONSTRAINT.VALID_VALUES", implementation = ValidValuesConstraint.class) })
    private List<PropertyConstraint> constraints;

    @BooleanField(index = IndexType.no)
    private boolean isPassword;

    public PropertyDefinition(PropertyDefinition from) {
        this.type = from.type;
        this.entrySchema = from.entrySchema;
        this.required = from.required;
        this.defaultValue = from.defaultValue;
        this.description = from.description;
        this.suggestionId = from.suggestionId;
        this.constraints = from.constraints;
        this.isPassword = from.isPassword;
    }

    @JsonDeserialize(using = PropertyValueDeserializer.class)
    @ObjectField(enabled = false)
    public PropertyValue getDefault() {
        return this.defaultValue;
    }

    public void setDefault(PropertyValue defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isDefinition() {
        return true;
    }

    /**
     * Check if all constraint are equals
     *
     * @param propertyDefinition
     * @throws IncompatiblePropertyDefinitionException
     */
    public void checkIfCompatibleOrFail(final PropertyDefinition propertyDefinition) throws IncompatiblePropertyDefinitionException {
        if (propertyDefinition == null) {
            throw new IncompatiblePropertyDefinitionException();
        } else if (!this.getType().equals(propertyDefinition.getType())) {
            throw new IncompatiblePropertyDefinitionException();
        } else if (this.getConstraints() == null && propertyDefinition.getConstraints() == null) {
            return;
        } else if (this.getConstraints() == null || propertyDefinition.getConstraints() == null
                || this.getConstraints().size() != propertyDefinition.getConstraints().size()) {
            throw new IncompatiblePropertyDefinitionException();
        }

        ArrayList<PropertyConstraint> copyOfOtherConstraints = new ArrayList<PropertyConstraint>(propertyDefinition.getConstraints());
        for (PropertyConstraint constraint : this.getConstraints()) {
            for (int i = 0; i <= copyOfOtherConstraints.size(); i++) {
                if (copyOfOtherConstraints.size() == 0) { // If all elements are compatible
                    return;
                } else if (i == copyOfOtherConstraints.size()) { // If the constraint is not compatible with an constraint from the other PropertyDefinition
                    throw new IncompatiblePropertyDefinitionException();
                } else if (constraint.equals(copyOfOtherConstraints.get(i))) { // If the two constraints are compatible
                    copyOfOtherConstraints.remove(i); // we remove the constraint in the copy of the other propertyDefinition constraints and continue
                    break;
                }
            }
        }
    }

}
