package alien4cloud.tosca.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.ui.form.annotation.FormProperties;

/**
 * A TOSCA function to be used as the value for a property (or operation parameter).
 */
@Getter
@Setter
@FormProperties({ "function", "parameters" })
public class FunctionPropertyValue extends AbstractPropertyValue {
    private String function;
    private List<String> parameters;
}