package alien4cloud.ui.form;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.Getter;
import lombok.Setter;

@FormProperties({ "c", "b", "a" })
@Getter
@Setter
public class FormPropertiesExampleObject {

    private String a;

    private String b;

    private String c;

    private String d;
}
