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
@Constraint(validatedBy = ToscaPropertyDefaultValueConstraintsValidator.class)
@Documented
public @interface ToscaPropertyDefaultValueConstraints {
    String message() default "CONSTRAINTS.VALIDATION.DEFAULT_VALUE_NOT_MATCH_CONSTRAINTS";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
