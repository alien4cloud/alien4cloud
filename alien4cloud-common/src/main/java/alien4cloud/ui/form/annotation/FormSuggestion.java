package alien4cloud.ui.form.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation contains information about suggestion that a property can have on the form
 * 
 * @author mkv
 * 
 */
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface FormSuggestion {

    /**
     * From which model's class, suggestion will be queried
     * 
     * @return
     */
    Class<?> fromClass();

    /**
     * the property path to retrieve available suggestions
     * 
     * @return
     */
    String path();
}
