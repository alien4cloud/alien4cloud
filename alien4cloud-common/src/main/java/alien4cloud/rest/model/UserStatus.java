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
    // Authentication system to be used - Alien, GitHub or SAML are the 3 available systems for now.
    private String authSystem;
}