package org.alien4cloud.secret.services;

import java.util.List;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;
import org.alien4cloud.secret.ISecretProvider;

@Component
public class SecretProviderRegistry extends AbstractPluginLinker<ISecretProvider> {

    @Override
    public List<PluginUsage> usage(String pluginId) {
        // TODO how to determine plugin usage
        return null;
    }
}
