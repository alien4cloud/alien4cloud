package org.alien4cloud.secret.services;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.model.SecretCredentialInfo;
import org.alien4cloud.secret.ISecretProvider;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
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
}
