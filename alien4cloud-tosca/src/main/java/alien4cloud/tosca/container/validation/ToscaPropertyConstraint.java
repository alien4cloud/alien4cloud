package alien4cloud.tosca.container.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ TYPE, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ToscaPropertyConstraintValidator.class)
@Documented
public @interface ToscaPropertyConstraint {
    String message() default "CONSTRAINTS.VALIDATION.ROOT";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}