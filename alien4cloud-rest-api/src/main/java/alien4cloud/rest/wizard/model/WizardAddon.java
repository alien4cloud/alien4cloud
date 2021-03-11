package alien4cloud.rest.wizard.model;

import alien4cloud.security.model.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * A wizard addon represent a webapp that can be acceded from wizard home page.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WizardAddon {
    private String id;
    private String iconName;
    private String[] roles;
    private Role[] authorizedRoles;
    private String contextPath;
}
