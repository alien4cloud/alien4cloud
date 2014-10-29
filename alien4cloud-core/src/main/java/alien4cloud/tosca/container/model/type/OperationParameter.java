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
    /** Value or value expression from tosca. */
    private String value;

    private String type;
    private boolean required;
}