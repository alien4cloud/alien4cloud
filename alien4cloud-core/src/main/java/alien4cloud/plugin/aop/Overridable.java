package alien4cloud.plugin.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to a type or a method, means that the bean can be overridden using AOP in plugin child context.
 * 
 * @see ChildContextAspectsManager
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Overridable {

}
