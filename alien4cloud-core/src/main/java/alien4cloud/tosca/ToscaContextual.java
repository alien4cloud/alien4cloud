package alien4cloud.tosca;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to ensure that a TOSCA context has been created for the next method.
 *
 * Note, one of the argument of the method should be a Topology or a Set of CSARDependencies that are required to create the TOSCA context.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ToscaContextual {
}