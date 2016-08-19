package alien4cloud.repository.services;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.repository.IArtifactResolver;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;

@Component
public class ResolverRegistry extends AbstractPluginLinker<IArtifactResolver> {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public List<PluginUsage> usage(String pluginId) {
        return RegistryUtil.getUsages(alienDAO, pluginId);
    }
}
