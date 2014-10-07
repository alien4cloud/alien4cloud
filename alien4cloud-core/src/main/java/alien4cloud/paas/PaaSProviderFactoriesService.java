package alien4cloud.paas;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.PluginUsage;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;

/**
 * Manages the IPaaSProvider plugin beans.
 */
@Component
public class PaaSProviderFactoriesService extends AbstractPluginLinker<IPaaSProviderFactory> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public List<PluginUsage> usage(String pluginId) {
        // query the list of clouds that uses the given plugin
        GetMultipleDataResult dataResult = alienDAO.search(Cloud.class, null,
                MapUtil.newHashMap(new String[] { "paasPluginId" }, new String[][] { new String[] { pluginId } }), Integer.MAX_VALUE);

        List<PluginUsage> usages = Lists.newArrayList();
        for (Object data : dataResult.getData()) {
            Cloud cloud = (Cloud) data;
            usages.add(new PluginUsage(cloud.getId(), cloud.getName(), "Cloud"));
        }

        return usages;
    }
}
