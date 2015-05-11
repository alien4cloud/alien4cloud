package alien4cloud.security.groups.rest;

import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class CreateGroupRequest {

    @NotBlank
    private String name;
    private String email;
    private String description;
    private Set<String> users;
    private Set<String> roles;

}
