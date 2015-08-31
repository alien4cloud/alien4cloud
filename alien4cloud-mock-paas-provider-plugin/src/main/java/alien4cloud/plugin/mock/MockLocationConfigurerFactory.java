package alien4cloud.plugin.mock;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import alien4cloud.model.cloud.IaaSType;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.plugin.IPluginContextAware;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.tosca.ArchiveParser;

/**
 * Component that creates location configurer for a mock openstack cloud.
 */
@Component
public class MockLocationConfigurerFactory implements IPluginContextAware {
    @Inject
    private ArchiveParser archiveParser;
    @Inject
    private PluginManager pluginManager;
    private ManagedPlugin selfContext;

    @Override
    public void setContext(ManagedPlugin selfContext) {
        this.selfContext = selfContext;
    }

    public ILocationConfiguratorPlugin newInstance(String locationType) {
        if (IaaSType.OPENSTACK.toString().equals(locationType)) {
            return new MockOpenStackLocationConfigurer(archiveParser, pluginManager, selfContext);
        }
        return new ILocationConfiguratorPlugin() {
            @Override
            public List<PluginArchive> pluginArchives() {
                return new ArrayList<>();
            }

            @Override
            public List<String> getResourcesTypes() {
                return new ArrayList<>();
            }

            @Override
            public List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) {
                return null;
            }
        };
    }
}