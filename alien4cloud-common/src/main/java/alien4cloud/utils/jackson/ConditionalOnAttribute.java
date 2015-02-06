package alien4cloud.utils.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This attributes is used by Conditional Serializers and De-serializers to specify which attribute on the context must exists so the de-serializer is enabled.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ConditionalOnAttribute {
    String[] value() default {};
}
