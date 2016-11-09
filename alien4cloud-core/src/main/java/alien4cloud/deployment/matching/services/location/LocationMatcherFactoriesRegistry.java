package alien4cloud.deployment.matching.services.location;

import java.util.List;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.deployment.matching.plugins.ILocationMatcher;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;

/**
 * Keeps track of the Location matchers and usages.
 */
@Component
public class LocationMatcherFactoriesRegistry extends AbstractPluginLinker<ILocationMatcher> {
    @Override
    public List<PluginUsage> usage(String pluginId) {
        // query the list of location matchers that uses the given plugin

        // TODO get usages of location matchers
        return Lists.newArrayList();
    }
}
