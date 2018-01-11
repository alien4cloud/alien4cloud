package alien4cloud.audit.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD, TYPE })
@Retention(RUNTIME)
public @interface Audit {

    boolean enabledByDefault() default true;

    String category() default "";

    String action() default "";

    String[] bodyHiddenFields() default {};
}
