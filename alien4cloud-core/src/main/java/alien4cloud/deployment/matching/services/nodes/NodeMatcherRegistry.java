package alien4cloud.deployment.matching.services.nodes;

import java.util.List;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;

/**
 * Registry that link INodeMatcherPlugin and checks usages.
 */
@Component
public class NodeMatcherRegistry extends AbstractPluginLinker<INodeMatcherPlugin> {
    @Override
    public List<PluginUsage> usage(String pluginId) {
        List<PluginUsage> usages = Lists.newArrayList();
        // TODO query for the plugin matcher to be used in the application (admin defined).

        return usages;
    }
}