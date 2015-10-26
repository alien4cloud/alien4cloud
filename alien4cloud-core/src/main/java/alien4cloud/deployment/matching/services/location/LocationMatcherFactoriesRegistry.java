package alien4cloud.deployment.matching.services.location;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.matching.plugins.ILocationMatcher;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;

import com.google.common.collect.Lists;

/**
 * Keeps track of the Location matchers and usages.
 */
@Component
public class LocationMatcherFactoriesRegistry extends AbstractPluginLinker<ILocationMatcher> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public List<PluginUsage> usage(String pluginId) {
        // query the list of location matchers that uses the given plugin

        // TODO get usages of location matchers
        return Lists.newArrayList();
    }
}
