package org.alien4cloud.secret;

import alien4cloud.model.secret.SecretAuthResponse;

/**
 * This interface represents a contract that must be respected by secret provider plugin
 * 
 * @param <T> the type of the configuration
 */
public interface ISecretProvider<T> {

    /**
     * Retrieve the class object of the configuration which permit to introspect and get the description of the configuration (fields' names, types, constraints
     * ...)
     * 
     * @return the class object of the configuration
     */
    Class<T> getConfigurationDescriptor();

    /**
     * Retrieve the class object of the authentication configuration which permit to introspect and get the description of the authentication configuration
     * (fields' names, types, constraints ...)
     * 
     * @param configuration the configuration of the secret provider in order to deduce the authentication configuration
     * @return the class object of the authentication configuration
     */
    Class<?> getAuthenticationConfigurationDescriptor(T configuration);

    /**
     * Use the secret provider configuration and the credentials object to authenticate and generate new configuration with new credentials if necessary.
     * Typically for Vault, we can authenticate with a ldap account and generate a token.
     *
     * @param configuration the secret provider configuration
     * @param credentials the credentials
     * @return the secret auth response which contains the new configuration and the new credentials
     */
    SecretAuthResponse auth(T configuration, Object credentials);
}
