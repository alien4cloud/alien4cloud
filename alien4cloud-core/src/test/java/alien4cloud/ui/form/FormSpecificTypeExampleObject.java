package alien4cloud.ui.form;

import java.util.Date;

import alien4cloud.ui.form.annotation.FormCustomType;
import alien4cloud.ui.form.annotation.FormProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FormProperties({ "specificTypeField", "normalDateField", "normalTextField", "normalIntField" })
public class FormSpecificTypeExampleObject {

    private int normalIntField;

    private String normalTextField;

    private Date normalDateField;

    @FormCustomType("specialOne")
    private Object specificTypeField;
}
