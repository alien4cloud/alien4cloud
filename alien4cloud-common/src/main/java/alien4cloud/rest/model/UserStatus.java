package alien4cloud.rest.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatus {
    private Boolean isLogged;
    private String username;
    private String githubUsername;
    private Collection<String> roles = new ArrayList<String>();
    private Set<String> groups;

}