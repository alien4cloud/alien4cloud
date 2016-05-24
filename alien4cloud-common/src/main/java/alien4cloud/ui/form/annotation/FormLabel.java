package alien4cloud.ui.form.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Label is shown on generic form instead of property name
 */
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface FormLabel {
    String value();
}
