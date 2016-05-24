package alien4cloud.ui.form.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation gives informations about which property will be included in the generated form,
 * in the same time it gives the ordering of the properties in the form. If not defined all properties
 * will be taken into account and order is not assured
 * 
 * @author mkv
 * 
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface FormProperties {

    String[] value();

}
