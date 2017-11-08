package org.alien4cloud.secret;

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
}
