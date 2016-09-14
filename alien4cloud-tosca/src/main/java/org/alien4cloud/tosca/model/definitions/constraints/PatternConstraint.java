package org.alien4cloud.tosca.model.definitions.constraints;

import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.regex.Pattern;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false, of = { "pattern" })
public class PatternConstraint extends AbstractStringPropertyConstraint {
    @NotNull
    private String pattern;
    @JsonIgnore
    private Pattern compiledPattern;

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.compiledPattern = Pattern.compile(this.pattern);
    }

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (!compiledPattern.matcher(propertyValue).matches()) {
            throw new ConstraintViolationException("The value do not match pattern " + pattern);
        }
    }
}