package alien4cloud.model.components;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.ui.form.annotation.FormProperties;

@Getter
@Setter
@FormProperties({ "function", "parameters" })
public class ConcatPropertyValue extends AbstractPropertyValue {
    private String function;
    private List<AbstractPropertyValue> parameters;
}
