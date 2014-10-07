package alien4cloud.ui.form;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FormProperties({ "a", "b", "c", "d" })
public class FormExtendBaseClassExampleObject extends FormPropertiesExampleObject {

    private String e;
}
