package org.alien4cloud.tosca.model.types;

import java.util.List;

import javax.validation.Valid;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.types.DataType;
import org.elasticsearch.annotation.ESObject;

import alien4cloud.json.deserializer.PropertyConstraintDeserializer;
import alien4cloud.tosca.container.validation.ToscaPropertyConstraintDuplicate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Complex data type used for property definition
 */
@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
@ESObject
public class PrimitiveDataType extends DataType {

    /**
     * Only data types that derive from a simple type have associated constraints.
     */
    @Valid
    @ToscaPropertyConstraintDuplicate
    @JsonDeserialize(contentUsing = PropertyConstraintDeserializer.class)
    private List<PropertyConstraint> constraints;

    public PrimitiveDataType() {
        super();
        this.setDeriveFromSimpleType(true);
    }

}
