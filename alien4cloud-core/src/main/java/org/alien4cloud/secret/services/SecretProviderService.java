package org.alien4cloud.secret.services;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.secret.ISecretProvider;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecretProviderService {

    @Resource
    private SecretProviderRegistry secretProviderRegistry;

    public Set<String> getAvailablePlugins() {
        return secretProviderRegistry.getInstancesByPlugins().keySet();
    }

    private ISecretProvider getPluginBean(String pluginName) {
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
}
