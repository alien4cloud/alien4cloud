package alien4cloud.ui.form.annotation;

/**
 * This annotations define the constraint imposed on a form field
 */
public @interface FormPropertyConstraint {

    String equal() default "";

    String greaterOrEqual() default "";

    String greaterThan() default "";

    /**
     * In the form of "2 - 3"
     */
    String inRange() default "";

    int length() default -1;

    int maxLength() default -1;

    int minLength() default -1;

    String lessOrEqual() default "";

    String lessThan() default "";

    String pattern() default "";

    String[] validValues() default {};
}
