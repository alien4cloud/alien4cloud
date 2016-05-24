package alien4cloud.ui.form.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A custom form type, then it will not be introspected
 * 
 * @author mkv
 * 
 */
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface FormCustomType {

    String value();
}
