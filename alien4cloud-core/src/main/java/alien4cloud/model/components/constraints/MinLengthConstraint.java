package alien4cloud.model.components.constraints;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, of = { "minLength" })
@Slf4j
public class MinLengthConstraint extends AbstractStringPropertyConstraint {
    @NotNull
    private Integer minLength;

    public MinLengthConstraint(Integer minLength) {
        this.minLength = minLength;
    }

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() < minLength) {
            throw new ConstraintViolationException("The length of the value is less than [" + minLength + "]");
        }
    }
}
