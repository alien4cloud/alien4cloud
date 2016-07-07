package alien4cloud.tosca.container.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ToscaPropertyTypeValidator.class)
@Documented
public @interface ToscaPropertyType {
    String message() default "CONSTRAINTS.VALIDATION.INVALID_TOSCA_TYPE";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
