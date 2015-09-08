package alien4cloud.orchestrators.locations.services;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.locations.ILocationMatcher;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;

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
        return null;
    }
}
