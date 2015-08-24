package alien4cloud.plugin.mock;

import java.util.ArrayList;
import java.util.List;

import alien4cloud.tosca.model.ArchiveRoot;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import alien4cloud.model.orchestrators.locations.LocationResourceDefinition;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;

/**
 * Mock implementation for an orchestrator instance.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MockOrchestrator extends MockPaaSProvider implements IOrchestratorPlugin<ProviderConfig> {
    @Override
    public ILocationConfiguratorPlugin getConfigurator(String locationType) {
        return new ILocationConfiguratorPlugin() {
            @Override
            public List<ArchiveRoot> pluginArchives() {
                return new ArrayList<>();
            }

            @Override
            public List<LocationResourceDefinition> definitions() {
                return new ArrayList<>();
            }

            @Override
            public List<LocationResourceTemplate> instances() {
                return null;
            }
        };
    }
}