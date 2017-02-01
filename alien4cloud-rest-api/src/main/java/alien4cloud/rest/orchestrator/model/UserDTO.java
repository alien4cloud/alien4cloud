package alien4cloud.rest.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserDTO {
    private String username;
    private String lastName;
    private String firstName;
    private String email;
}