package org.alien4cloud.alm.deployment.configuration.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecretCredentialInfo {
    // contains the structure of the credential
    // token -> 1 string (token)
    // ldap -> 2 string (user/password)
    private Object credentialDescriptor;
    // name of the plugin (captain obvious)
    private String pluginName;
}
