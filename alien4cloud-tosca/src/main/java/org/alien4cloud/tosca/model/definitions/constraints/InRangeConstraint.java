package org.alien4cloud.tosca.model.definitions.constraints;

import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.ui.form.annotation.FormProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings({ "unchecked", "rawtypes" })
@EqualsAndHashCode(callSuper = false, of = { "inRange" })
@FormProperties({ "rangeMinValue", "rangeMaxValue" })
public class InRangeConstraint extends AbstractPropertyConstraint {

    @Getter
    @Setter
    private List<String> inRange;

    private Comparable min;
    private Comparable max;

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        // Perform verification that the property type is supported for comparison
        ConstraintUtil.checkComparableType(propertyType);
        if (inRange == null || inRange.size() != 2) {
            throw new ConstraintValueDoNotMatchPropertyTypeException("In range constraint must have two elements.");
        }
        String minRawText = inRange.get(0);
        String maxRawText = inRange.get(1);
        min = ConstraintUtil.convertToComparable(propertyType, minRawText);
        max = ConstraintUtil.convertToComparable(propertyType, maxRawText);
    }

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to check is null");
        }
        if (!(min.getClass().isAssignableFrom(propertyValue.getClass()))) {
            throw new ConstraintViolationException(
                    "Value to check is not comparable to range type, value type [" + propertyValue.getClass() + "], range type [" + min.getClass() + "]");
        }
        if (min.compareTo(propertyValue) > 0 || max.compareTo(propertyValue) < 0) {
            throw new ConstraintViolationException("The value [" + propertyValue + "] is out of range " + inRange);
        }
    }

    @JsonProperty
    @NotNull
    public String getRangeMinValue() {
        if (inRange != null) {
            return inRange.get(0);
        } else {
            return null;
        }
    }

    @JsonProperty
    public void setRangeMinValue(String minValue) {
        if (inRange == null) {
            inRange = Lists.newArrayList(minValue, "");
        } else {
            inRange.set(0, minValue);
        }
    }

    @JsonProperty
    @NotNull
    public String getRangeMaxValue() {
        if (inRange != null) {
            return inRange.get(1);
        } else {
            return null;
        }
    }

    @JsonProperty
    public void setRangeMaxValue(String maxValue) {
        if (inRange == null) {
            inRange = Lists.newArrayList("", maxValue);
        } else {
            inRange.set(1, maxValue);
        }
    }
}
