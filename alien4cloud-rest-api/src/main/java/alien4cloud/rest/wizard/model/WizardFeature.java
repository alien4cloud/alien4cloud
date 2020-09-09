package alien4cloud.rest.wizard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * A feature link in the wizard home page.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WizardFeature {
    private String id;
    private String iconName;
    private String activationLink;
    private boolean allowed;
    private boolean enabled;
}
