package alien4cloud.rest.utils;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Ensure that the value of a patch rest request is not json null.
 */
public class PatchNotBlankValidator implements ConstraintValidator<PatchNotBlankValidator.PatchNotBlank, Object> {

    @Override
    public void initialize(PatchNotBlank patchNotNull) {
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        if (o == null) {
            return true; // match javascript undefined which means do not patch.
        }
        if (o == RestMapper.NULL_INSTANCES.get(o.getClass())) {
            return false; // javascript set to null not valid
        }
        if (o instanceof String && ((String) o).isEmpty()) {
            return false;
        }
        if (o instanceof List && ((List) o).isEmpty()) {
            return false;
        }
        return true;
    }

    @Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = PatchNotBlankValidator.class)
    @Documented
    public @interface PatchNotBlank {
        String message() default "{org.hibernate.validator.constraints.NotBlank.message}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }
}
