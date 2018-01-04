package org.alien4cloud.secret.services;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.model.SecretCredentialInfo;
import org.alien4cloud.secret.ISecretProvider;
import org.springframework.stereotype.Component;

import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.deployment.model.SecretProviderCredentials;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.secret.SecretAuthResponse;
import alien4cloud.model.secret.SecretProviderConfiguration;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.ui.form.PojoFormDescriptorGenerator;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecretProviderService {

    @Resource
    private SecretProviderRegistry secretProviderRegistry;
    @Inject
    private PojoFormDescriptorGenerator pojoFormDescriptorGenerator;

    public Set<String> getAvailablePlugins() {
        return secretProviderRegistry.getInstancesByPlugins().keySet();
    }

    public ISecretProvider getPluginBean(String pluginName) {
        Map<String, ISecretProvider> secretBeansMap = secretProviderRegistry.getInstancesByPlugins().get(pluginName);
        if (secretBeansMap == null) {
            throw new NotFoundException("No secret provider plugin is found in the system with name [" + pluginName + "]");
        } else {
            return secretBeansMap.values().iterator().next();
        }
    }

    public Class<?> getPluginConfigurationDescriptor(String pluginName) {
        return getPluginBean(pluginName).getConfigurationDescriptor();
    }

    public Class<?> getPluginAuthenticationConfigurationDescriptor(String pluginName, Object pluginConfiguration) {
        return getPluginBean(pluginName).getAuthenticationConfigurationDescriptor(pluginConfiguration);
    }

    public Object getPluginConfiguration(String pluginName, Object rawSecretConfiguration) {
        return JsonUtil.toObject(rawSecretConfiguration, getPluginConfigurationDescriptor(pluginName));
    }

    public Object getCredentials(String pluginName, Object pluginConfiguration, Object rawCredentials) {
        return JsonUtil.toObject(rawCredentials, getPluginAuthenticationConfigurationDescriptor(pluginName, pluginConfiguration));
    }

    public SecretCredentialInfo getSecretCredentialInfo(String pluginName, Object rawSecretConfiguration) {
        SecretCredentialInfo info = new SecretCredentialInfo();
        Object secretConfiguration = getPluginConfiguration(pluginName, rawSecretConfiguration);
        Class<?> pluginAuthenticationConfigurationDescriptor = getPluginAuthenticationConfigurationDescriptor(pluginName, secretConfiguration);
        info.setCredentialDescriptor(pojoFormDescriptorGenerator.generateDescriptor(pluginAuthenticationConfigurationDescriptor));
        info.setPluginName(pluginName);
        return info;
    }

    /**
     * This method is used to build a valid object who contains the configuration of the secret provider from the location and the credentials for the current
     * request/operation
     *
     * @param locations map of locations
     * @param secretProviderCredentials the secret provider credentials configuration
     * @return the more suitable model for orchestrator
     */
    public SecretProviderConfigurationAndCredentials generateSecretConfiguration(Map<String, Location> locations,
            SecretProviderCredentials secretProviderCredentials) {
        if (secretProviderCredentials == null) {
            return null;
        }
        return generateSecretConfiguration(locations, secretProviderCredentials.getPluginName(), secretProviderCredentials.getCredentials());
    }

    /**
     * Generate a token wrapped in the new instance of SecretProviderConfigurationAndCredentials
     * @param locations
     * @param pluginName
     * @param credentials
     * @return new instance of SecretProviderConfigurationAndCredentials wrapping the token
     */
    public SecretProviderConfigurationAndCredentials generateSecretConfiguration(Map<String, Location> locations, String pluginName, Object credentials) {
        if (credentials == null) {
            return null;
        }
        Optional<Location> firstLocation = locations.values().stream()
                .filter(location -> Objects.equals(pluginName, location.getSecretProviderConfiguration().getPluginName())).findFirst();
        if (!firstLocation.isPresent()) {
            log.error("Plugin name <" + pluginName + "> is not configured by the current location.");
            return null;
        }
        return generateSecretConfiguration(firstLocation.get().getSecretProviderConfiguration(), credentials);
    }

    /**
     * Generate a token wrapped in an instance of SecretProviderConfigurationAndCredentials by authenticating the credentials (username, password) with ldap
     * @param locationConfiguration
     * @param credentials
     * @return SecretProviderConfigurationAndCredentials wrapping a token
     */
    private SecretProviderConfigurationAndCredentials generateSecretConfiguration(SecretProviderConfiguration locationConfiguration, Object credentials) {
        if(credentials == null) {
            return null;
        }
        // Instead of saving the credentials username and password, we transform the username and password to a client token
        ISecretProvider secretProvider = this.getPluginBean(locationConfiguration.getPluginName());
        Object configuration = this.getPluginConfiguration(locationConfiguration.getPluginName(), locationConfiguration.getConfiguration());
        SecretAuthResponse authResponse = secretProvider.auth(configuration,
                this.getCredentials(locationConfiguration.getPluginName(), configuration, credentials));
        SecretProviderConfigurationAndCredentials result = new SecretProviderConfigurationAndCredentials();
        SecretProviderConfiguration secretProviderConfiguration = new SecretProviderConfiguration();
        secretProviderConfiguration.setPluginName(locationConfiguration.getPluginName());
        secretProviderConfiguration.setConfiguration(authResponse.getConfiguration());
        result.setSecretProviderConfiguration(secretProviderConfiguration);
        result.setCredentials(authResponse.getCredentials());
        return result;
    }

}
