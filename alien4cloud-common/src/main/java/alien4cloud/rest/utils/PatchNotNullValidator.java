package alien4cloud.rest.utils;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Ensure that the value of a patch rest request is not json null.
 */
public class PatchNotNullValidator implements ConstraintValidator<PatchNotNullValidator.PatchNotNull, Object> {

    @Override
    public void initialize(PatchNotNull patchNotNull) {
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        if (o == null) {
            return true; // match javascript undefined which means do not patch.
        }
        return !(o == RestMapper.NULL_INSTANCES.get(o.getClass()));
    }

    @Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = PatchNotNullValidator.class)
    @Documented
    public @interface PatchNotNull {
        String message() default "{org.hibernate.validator.constraints.NotBlank.message}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }
}
