package alien4cloud.security;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CreateUserRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String lastName;
    private String firstName;
    private String email;
    private String[] roles;
}