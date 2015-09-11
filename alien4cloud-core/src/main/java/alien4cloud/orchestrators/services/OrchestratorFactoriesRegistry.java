package alien4cloud.orchestrators.services;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;

/**
 * Keeps track of the orchestrator plugins and usages.
 */
@Component
public class OrchestratorFactoriesRegistry extends AbstractPluginLinker<IOrchestratorPluginFactory> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public List<PluginUsage> usage(String pluginId) {
        // query the list of orchestrators that uses the given plugin
        GetMultipleDataResult<Orchestrator> dataResult = alienDAO.search(Orchestrator.class, null,
                MapUtil.newHashMap(new String[] { "pluginId" }, new String[][] { new String[] { pluginId } }), Integer.MAX_VALUE);

        List<PluginUsage> usages = Lists.newArrayList();
        for (Orchestrator orchestrator : dataResult.getData()) {
            usages.add(new PluginUsage(orchestrator.getId(), orchestrator.getName(), Orchestrator.class.getSimpleName()));
        }

        return usages;
    }
}
