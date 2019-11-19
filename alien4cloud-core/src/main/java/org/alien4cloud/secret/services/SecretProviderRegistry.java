package org.alien4cloud.secret.services;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.secret.SecretProviderConfiguration;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;
import com.google.common.collect.Lists;
import org.alien4cloud.secret.ISecretProvider;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class SecretProviderRegistry extends AbstractPluginLinker<ISecretProvider> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public List<PluginUsage> usage(String pluginId) {
        GetMultipleDataResult<Location> locationData = alienDAO.buildQuery(Location.class).prepareSearch().search(0, 10000);

        List<PluginUsage> usages = Lists.newArrayList();
        for (Location location : locationData.getData()) {
            SecretProviderConfiguration locationSecretProviderConfiguration = location.getSecretProviderConfiguration();
            if (locationSecretProviderConfiguration != null && locationSecretProviderConfiguration.getPluginName().equals(pluginId)) {
                usages.add(new PluginUsage(location.getId(), location.getName(), Location.class.getSimpleName()));

            }
        }

        return usages;
    }
}
