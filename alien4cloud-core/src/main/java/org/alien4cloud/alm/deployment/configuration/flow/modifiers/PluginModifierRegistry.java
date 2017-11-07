package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import java.util.List;

import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;

/**
 * Registry of plugin modifiers.
 */
@Component
public class PluginModifierRegistry extends AbstractPluginLinker<ITopologyModifier> {
    @Override
    public List<PluginUsage> usage(String pluginId) {
        // FIXME look for policies that leverages ITopologyModifiers as implementations
        // FIXME look for locations that leverages the modifier in their modifier list
        return Lists.newArrayList();
    }
}