package alien4cloud.repository.services;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.repository.IConfigurableArtifactResolverFactory;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;

@Component
public class ConfigurableResolverRegistry extends AbstractPluginLinker<IConfigurableArtifactResolverFactory> {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public List<PluginUsage> usage(String pluginId) {
        return RegistryUtil.getUsages(alienDAO, pluginId);
    }
}
