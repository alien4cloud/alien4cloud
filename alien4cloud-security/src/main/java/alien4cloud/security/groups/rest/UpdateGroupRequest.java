package alien4cloud.security.groups.rest;

import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateGroupRequest {
    private String name;
    private String email;
    private String description;
    private Set<String> users;
    private Set<String> roles;
}
