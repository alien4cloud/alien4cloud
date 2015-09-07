package alien4cloud.topology.matching.nodes.services;

import java.util.List;

import javax.annotation.Resource;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;
import alien4cloud.topology.matching.nodes.plugin.INodeMatcherPlugin;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

/**
 * Registry that link INodeMatcherPlugin and checks usages.
 */
@Component
public class NodeMatcherRegistry extends AbstractPluginLinker<INodeMatcherPlugin> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public List<PluginUsage> usage(String pluginId) {
        List<PluginUsage> usages = Lists.newArrayList();
        // TODO query for the plugin matcher to be used in the application (admin defined).

        return usages;
    }
}