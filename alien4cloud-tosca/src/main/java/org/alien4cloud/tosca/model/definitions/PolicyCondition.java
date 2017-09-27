package org.alien4cloud.tosca.model.definitions;

import java.util.List;

import javax.validation.Valid;

import org.elasticsearch.annotation.ObjectField;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.json.deserializer.PropertyConstraintDeserializer;
import alien4cloud.tosca.container.validation.ToscaPropertyConstraintDuplicate;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines the condition to be applied on the policy attribute.
 */
@Getter
@Setter
public class PolicyCondition {
    @ObjectField(enabled = false)
    @Valid
    @ToscaPropertyConstraintDuplicate
    @JsonDeserialize(contentUsing = PropertyConstraintDeserializer.class)
    private List<PropertyConstraint> constraints;
    private String period;
    private int evaluations = 1;
    private String method;
}
