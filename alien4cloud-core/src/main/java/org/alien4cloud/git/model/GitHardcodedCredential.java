package org.alien4cloud.git.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GitHardcodedCredential implements GitCredential {
    private String username;
    private String password;
}
