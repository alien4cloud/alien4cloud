package org.alien4cloud.alm.deployment.configuration.flow;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

import static alien4cloud.utils.AlienUtils.safe;

@Component
public class TopologyModifierPluginLinker extends AbstractPluginLinker<ITopologyModifier> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public List<PluginUsage> usage(String pluginId) {
        // Get all modifiers associated with a location
        GetMultipleDataResult<Location> locationData = alienDAO.buildQuery(Location.class).prepareSearch().search(0, 10000);

        List<PluginUsage> usages = Lists.newArrayList();
        for (Location location : locationData.getData()) {
            for (LocationModifierReference locationModifierReference : safe(location.getModifiers())) {
                if (pluginId.equals(locationModifierReference.getPluginId())) {
                    usages.add(new PluginUsage(location.getId(), location.getName(), Location.class.getSimpleName()));
                }
            }
        }

        return usages;
    }
}
