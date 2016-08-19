package alien4cloud.repository.services;

import java.util.List;

import com.google.common.collect.Lists;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.repository.Repository;
import alien4cloud.plugin.model.PluginUsage;
import alien4cloud.utils.MapUtil;

public class RegistryUtil {

    public static List<PluginUsage> getUsages(IGenericSearchDAO alienDAO, String pluginId) {
        // query the list of repositories that uses the given plugin
        GetMultipleDataResult<Repository> dataResult = alienDAO.search(Repository.class, null,
                MapUtil.newHashMap(new String[] { "pluginId" }, new String[][] { new String[] { pluginId } }), Integer.MAX_VALUE);

        List<PluginUsage> usages = Lists.newArrayList();
        for (Repository repository : dataResult.getData()) {
            usages.add(new PluginUsage(repository.getId(), repository.getName(), Repository.class.getSimpleName()));
        }

        return usages;
    }
}
