package alien4cloud.tosca.container.model.type;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * An input or output parameter for a {@link alien4cloud.tosca.container.model.template.Plan plan} or an {@link Operation operation}.
 *
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@FormProperties({ "type", "required" })
public class OperationParameter {
    /**
     * This attribute specifies the type of the parameter.
     */
    private String type;
    /**
     * This OPTIONAL attribute specifies whether or not the input parameter is REQUIRED (required attribute with a value of “true�? – default) or OPTIONAL
     * (required attribute with a value of “false�?).
     */
    private boolean required = true;
}
