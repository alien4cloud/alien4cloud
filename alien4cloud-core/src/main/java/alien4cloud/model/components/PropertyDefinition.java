package alien4cloud.model.components;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.json.deserializer.PropertyConstraintDeserializer;
import alien4cloud.model.components.constraints.EqualConstraint;
import alien4cloud.model.components.constraints.GreaterOrEqualConstraint;
import alien4cloud.model.components.constraints.GreaterThanConstraint;
import alien4cloud.model.components.constraints.InRangeConstraint;
import alien4cloud.model.components.constraints.LengthConstraint;
import alien4cloud.model.components.constraints.LessOrEqualConstraint;
import alien4cloud.model.components.constraints.LessThanConstraint;
import alien4cloud.model.components.constraints.MaxLengthConstraint;
import alien4cloud.model.components.constraints.MinLengthConstraint;
import alien4cloud.model.components.constraints.PatternConstraint;
import alien4cloud.model.components.constraints.ValidValuesConstraint;
import alien4cloud.tosca.container.validation.ToscaPropertyConstraint;
import alien4cloud.tosca.container.validation.ToscaPropertyConstraintDuplicate;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueConstraints;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueType;
import alien4cloud.tosca.container.validation.ToscaPropertyPostValidationGroup;
import alien4cloud.tosca.container.validation.ToscaPropertyType;
import alien4cloud.ui.form.annotation.FormContentTypes;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormType;
import alien4cloud.ui.form.annotation.FormValidValues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "type", "required", "description", "defaultValue", "constraints" })
@SuppressWarnings("PMD.UnusedPrivateField")
@ToscaPropertyDefaultValueType
@ToscaPropertyConstraint
@ToscaPropertyDefaultValueConstraints(groups = { ToscaPropertyPostValidationGroup.class })
@JsonIgnoreProperties(ignoreUnknown = true)
@FormProperties({ "type", "required", "default", "description" })
public class PropertyDefinition implements IOperationParameter {
    @ToscaPropertyType
    @FormValidValues({ "boolean", "string", "float", "integer", "version" })
    @NotNull
    private String type;

    @NotNull
    private boolean required = true;

    @JsonProperty("default")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String defaultValue;

    private String description;

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

    private Schema entrySchema;

    private boolean isPassword;

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

    public void checkIfCompatibleOrFail(PropertyDefinition propertyDefinition) throws IncompatiblePropertyDefinitionException {
        List<PropertyConstraint> otherConstraints = propertyDefinition.getConstraints();
        if (this.getConstraints() == null || otherConstraints == null || this.getConstraints().size() != otherConstraints.size()) {
            throw new IncompatiblePropertyDefinitionException("The PropertyDefinition " + propertyDefinition.getType() + "is incompatible whit "
                    + this.getType());
        }

        for (PropertyConstraint property : this.getConstraints()) {
            for (int i = 0; i <= otherConstraints.size(); i++) {
                if (otherConstraints.size() == 0) {
                    break;
                } else if (i == otherConstraints.size()) {
                    throw new IncompatiblePropertyDefinitionException("The PropertyDefinition " + propertyDefinition.getType() + "is incompatible whit "
                            + this.getType());
                } else if (property.equals(otherConstraints.get(i))) {
                    otherConstraints.remove(i);
                    break;
                }
            }
        }
    }

}
