package alien4cloud.ui.form.annotation;

public @interface FormType {

    String label();

    String discriminantProperty();

    Class implementation();
}
