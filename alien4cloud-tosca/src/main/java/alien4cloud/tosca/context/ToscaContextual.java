package alien4cloud.tosca.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to ensure that a TOSCA context has been created for the next method.
 *
 * Note, one of the argument of the method should be a Topology or a Set of CSARDependencies that are required to create the TOSCA context.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ToscaContextual {
    /**
     * By default the tosca context is reused if one already exists. When requiresNew is true a new one is created even if an existing Tosca Context exists.
     */
    boolean requiresNew() default false;
}