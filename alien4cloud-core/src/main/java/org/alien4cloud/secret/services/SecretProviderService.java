package org.alien4cloud.secret.services;

import alien4cloud.exception.NotFoundException;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.ui.form.PojoFormDescriptorGenerator;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.deployment.configuration.model.SecretCredentialInfo;
import org.alien4cloud.secret.ISecretProvider;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

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

    public SecretCredentialInfo getSecretCredentialInfo(String pluginName, Object rawSecretConfiguration) {
        SecretCredentialInfo info = new SecretCredentialInfo();
        Object secretConfiguration = JsonUtil.toObject(rawSecretConfiguration, getPluginConfigurationDescriptor(pluginName));
        Class<?> pluginAuthenticationConfigurationDescriptor = getPluginAuthenticationConfigurationDescriptor(pluginName, secretConfiguration);
        info.setCredentialDescriptor(pojoFormDescriptorGenerator.generateDescriptor(pluginAuthenticationConfigurationDescriptor));
        info.setPluginName(pluginName);
        return info;
    }
}
